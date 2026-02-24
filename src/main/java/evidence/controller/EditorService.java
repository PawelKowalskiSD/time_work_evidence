package evidence.controller;

import evidence.model.VacationEvent;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorService {
    private static final double POOL_CONVERSION_FACTOR = 7.0 + (35.0 / 60.0);

    public interface ProgressListener {
        void update(String status, int current, int total);
    }

    public void processFilesChain(List<File> files, int startOverdueDays, int baseDimStartOfYear, List<VacationEvent> events, ProgressListener listener) {
        files.sort((f1, f2) -> Integer.compare(extractYear(f1), extractYear(f2)));

        double currentOverdueHours = startOverdueDays * POOL_CONVERSION_FACTOR;
        double currentOvertimeBalance = 0.0;

        int count = 0;
        for (File file : files) {
            listener.update("Przetwarzanie: " + file.getName(), count, files.size());
            try {
                int year = extractYear(file);
                double[] results = updateSingleFile(file, year, currentOverdueHours, currentOvertimeBalance, baseDimStartOfYear, events);
                currentOverdueHours = results[0];
                currentOvertimeBalance = results[1];
            } catch (Exception e) {
                e.printStackTrace();
            }
            count++;
        }
        listener.update("Zakonczono.", count, count);
    }

    private double[] updateSingleFile(File file, int year, double startOverdueHours, double startOvertimeBalance, int baseDimStartOfYear, List<VacationEvent> events) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            List<MonthStatus> yearMap = buildYearMap(workbook, year, baseDimStartOfYear, events);
            String poolDesc = generateDescription(yearMap);

            int detectedStartMonth = detectStartMonth(workbook);
            double currentCurrent = calculateComplexPool(yearMap);

            double currentOverdue = startOverdueHours;
            double runningOvertimeBalance = startOvertimeBalance;

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                int mIdx = i + 1;

                if (mIdx < detectedStartMonth) continue;

                clearOldTablesArea(sheet, 41, 45);

                double usedVac = calculateUsedVacation(sheet);
                double currentFte = yearMap.get(i).fte;
                double[] normParts = calculateNormParts(year, mIdx, currentFte);

                boolean isFirstMonth = (mIdx == detectedStartMonth);
                String prevSheetName = (i > 0) ? workbook.getSheetAt(i - 1).getSheetName() : null;

                drawRequestedTables(sheet, 41, normParts, currentOverdue, currentCurrent, runningOvertimeBalance, poolDesc, isFirstMonth, prevSheetName);

                double takeFromOverdue = Math.min(usedVac, currentOverdue);
                currentOverdue -= takeFromOverdue;
                currentCurrent -= (usedVac - takeFromOverdue);

                double act = calculateSumColumnF(sheet);
                double abs = calculateSumAbsences(sheet);
                double totalNorm = normParts[0] + normParts[1];
                runningOvertimeBalance += (act + abs - totalNorm);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            return new double[]{currentOverdue + currentCurrent, runningOvertimeBalance};
        }
    }

    private String generateDescription(List<MonthStatus> map) {
        StringBuilder sb = new StringBuilder();
        double lastFte = -1;
        int lastDim = -1;

        for (MonthStatus ms : map) {
            if (Math.abs(ms.fte - lastFte) > 0.001 || ms.dimension != lastDim) {
                sb.append(String.format("Od m-ca %d: etat %s, %d dni; ", ms.monthIndex, ms.fteText, ms.dimension));
                lastFte = ms.fte;
                lastDim = ms.dimension;
            }
        }
        return sb.toString();
    }

    private void drawRequestedTables(Sheet sheet, int startRow, double[] normParts, double initOverdue, double yearPool, double initOvertime, String note, boolean isFirstMonth, String prevSheet) {
        Workbook wb = sheet.getWorkbook();

        CellStyle baseBorder = wb.createCellStyle();
        baseBorder.setBorderBottom(BorderStyle.THIN);
        baseBorder.setBorderTop(BorderStyle.THIN);
        baseBorder.setBorderLeft(BorderStyle.THIN);
        baseBorder.setBorderRight(BorderStyle.THIN);
        baseBorder.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle greyBold11 = wb.createCellStyle();
        greyBold11.cloneStyleFrom(baseBorder);
        greyBold11.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        greyBold11.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font11 = wb.createFont();
        font11.setBold(true);
        font11.setFontHeightInPoints((short) 11);
        greyBold11.setFont(font11);

        CellStyle greyCenter11 = wb.createCellStyle();
        greyCenter11.cloneStyleFrom(greyBold11);
        greyCenter11.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dataStyle = wb.createCellStyle();
        dataStyle.cloneStyleFrom(baseBorder);
        dataStyle.setDataFormat(wb.createDataFormat().getFormat("[h]:mm"));
        dataStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle labelStyle = wb.createCellStyle();
        labelStyle.cloneStyleFrom(baseBorder);
        labelStyle.setAlignment(HorizontalAlignment.LEFT);

        CellStyle noteStyle = wb.createCellStyle();
        noteStyle.cloneStyleFrom(baseBorder);
        noteStyle.setAlignment(HorizontalAlignment.CENTER);
        noteStyle.setWrapText(true);
        Font sFont = wb.createFont();
        sFont.setFontHeightInPoints((short) 8);
        noteStyle.setFont(sFont);

        float h17px = 12.75f;
        getOrCreateRow(sheet, startRow + 3).setHeightInPoints(h17px);
        getOrCreateRow(sheet, startRow + 4).setHeightInPoints(h17px);

        Row r42 = getOrCreateRow(sheet, startRow);
        createCell(r42, 0, "*)", null);
        createMergedCell(sheet, r42, 1, 8, "naleza wypelnic po zakonczeniu danego okresu rozliczeniowego", greyBold11);
        createMergedCell(sheet, r42, 9, 10, "GODZINY", greyCenter11);
        createMergedCell(sheet, r42, 12, 14, "KAT.", greyCenter11);
        createMergedCell(sheet, r42, 15, 17, "BILANS POCZATKOWY", greyCenter11);
        createMergedCell(sheet, r42, 18, 20, "WYKAZ", greyCenter11);
        createMergedCell(sheet, r42, 21, 23, "SALDO NA KONIEC MIESIACA", greyCenter11);

        Row r43 = getOrCreateRow(sheet, startRow + 1);
        createMergedCell(sheet, r43, 1, 6, "Normatywny czas pracy za okres rozliczeniowy", greyBold11);
        createValueCell(r43, 7, normParts[0], dataStyle);
        createValueCell(r43, 8, normParts[1], dataStyle);
        createFormulaMergedCell(sheet, r43, 9, 10, "SUM(H43:I43)", dataStyle);
        createMergedCell(sheet, r43, 12, 14, "URLOP ZALEGLY", greyCenter11);

        if (isFirstMonth) createValueMergedCell(sheet, r43, 15, 17, initOverdue, dataStyle);
        else createFormulaMergedCell(sheet, r43, 15, 17, "'" + prevSheet + "'!V43", dataStyle);

        createFormulaMergedCell(sheet, r43, 18, 20, "MIN(P43,SUM(L9:L39))", dataStyle);
        createFormulaMergedCell(sheet, r43, 21, 23, "P43-S43", dataStyle);

        Row r44 = getOrCreateRow(sheet, startRow + 2);
        createMergedCell(sheet, r44, 1, 8, "Faktyczny czas pracy pracownika", greyBold11);
        createFormulaMergedCell(sheet, r44, 9, 10, "F40", dataStyle);
        createMergedCell(sheet, r44, 12, 14, "URLOP BIEZACY", greyCenter11);

        if (isFirstMonth) createValueMergedCell(sheet, r44, 15, 17, yearPool, dataStyle);
        else createFormulaMergedCell(sheet, r44, 15, 17, "'" + prevSheet + "'!V44", dataStyle);

        createFormulaMergedCell(sheet, r44, 18, 20, "MAX(0,SUM(L9:L39)-S43)", dataStyle);
        createFormulaMergedCell(sheet, r44, 21, 23, "P44-S44", dataStyle);

        Row r45 = getOrCreateRow(sheet, startRow + 3);
        createMergedCell(sheet, r45, 1, 8, "Czas zaliczany do czasu pracy pracownika (urlopy, del. itp..)", greyBold11);
        createFormulaMergedCell(sheet, r45, 9, 10, "SUM(L40:T40)", dataStyle);
        createMergedCell(sheet, r45, 12, 14, "NADGODZINY", greyCenter11);

        if (isFirstMonth) createValueMergedCell(sheet, r45, 15, 17, initOvertime, dataStyle);
        else createFormulaMergedCell(sheet, r45, 15, 17, "'" + prevSheet + "'!V45", dataStyle);

        createFormulaMergedCell(sheet, r45, 18, 20, "J46-J43", dataStyle);
        createFormulaMergedCell(sheet, r45, 21, 23, "P45+S45", dataStyle);

        Row r46 = getOrCreateRow(sheet, startRow + 4);
        createCell(r46, 0, "*)", null);
        CellStyle rightBold = wb.createCellStyle();
        rightBold.cloneStyleFrom(greyBold11);
        rightBold.setAlignment(HorizontalAlignment.RIGHT);
        createMergedCell(sheet, r46, 1, 8, "RAZEM", rightBold);
        createFormulaMergedCell(sheet, r46, 9, 10, "J44+J45", dataStyle);
        createMergedCell(sheet, r46, 12, 14, "NOTATKA", greyCenter11);
        createMergedCell(sheet, r46, 15, 23, note, noteStyle);
    }

    private double[] calculateNormParts(int year, int month, double fte) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();
        int fullWeeks = daysInMonth / 7;
        double fullWeeksHours = (fullWeeks * 5 * POOL_CONVERSION_FACTOR) * fte;
        double remHours = 0;
        for (int d = (fullWeeks * 7) + 1; d <= daysInMonth; d++) {
            LocalDate date = LocalDate.of(year, month, d);
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) remHours += POOL_CONVERSION_FACTOR;
        }
        double holidayHours = 0;
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = LocalDate.of(year, month, d);
            if (isPolishHoliday(date) && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                holidayHours += POOL_CONVERSION_FACTOR;
            }
        }
        double finalI43 = (remHours * fte) - (holidayHours * fte);
        return new double[]{fullWeeksHours, finalI43};
    }

    private double calculateComplexPool(List<MonthStatus> map) {
        double totalDays = 0;
        for (MonthStatus ms : map) {
            double annualForFte = Math.ceil(ms.dimension * ms.fte);
            totalDays += annualForFte / 12.0;
        }
        return Math.ceil(totalDays) * POOL_CONVERSION_FACTOR;
    }

    private void createCell(Row r, int c, String v, CellStyle s) {
        Cell cell = r.getCell(c);
        if (cell == null) cell = r.createCell(c);
        cell.setCellValue(v);
        if (s != null) cell.setCellStyle(s);
    }

    private void createValueCell(Row r, int c, double v, CellStyle s) {
        Cell cell = r.getCell(c);
        if (cell == null) cell = r.createCell(c);
        cell.setCellValue(v / 24.0);
        cell.setCellStyle(s);
    }

    private void createFormulaCell(Row r, int c, String f, CellStyle s) {
        Cell cell = r.getCell(c);
        if (cell == null) cell = r.createCell(c);
        cell.setCellFormula(f);
        cell.setCellStyle(s);
    }

    private void createMergedCell(Sheet s, Row r, int c1, int c2, String v, CellStyle st) {
        Cell cell = r.createCell(c1);
        cell.setCellValue(v);
        cell.setCellStyle(st);
        for (int i = c1 + 1; i <= c2; i++) {
            if (r.getCell(i) == null) r.createCell(i);
            r.getCell(i).setCellStyle(st);
        }
        s.addMergedRegion(new CellRangeAddress(r.getRowNum(), r.getRowNum(), c1, c2));
    }

    private void createValueMergedCell(Sheet s, Row r, int c1, int c2, double v, CellStyle st) {
        Cell cell = r.createCell(c1);
        cell.setCellValue(v / 24.0);
        cell.setCellStyle(st);
        for (int i = c1 + 1; i <= c2; i++) {
            if (r.getCell(i) == null) r.createCell(i);
            r.getCell(i).setCellStyle(st);
        }
        s.addMergedRegion(new CellRangeAddress(r.getRowNum(), r.getRowNum(), c1, c2));
    }

    private void createFormulaMergedCell(Sheet s, Row r, int c1, int c2, String f, CellStyle st) {
        Cell cell = r.createCell(c1);
        cell.setCellFormula(f);
        cell.setCellStyle(st);
        for (int i = c1 + 1; i <= c2; i++) {
            if (r.getCell(i) == null) r.createCell(i);
            r.getCell(i).setCellStyle(st);
        }
        s.addMergedRegion(new CellRangeAddress(r.getRowNum(), r.getRowNum(), c1, c2));
    }

    private Row getOrCreateRow(Sheet s, int idx) {
        Row r = s.getRow(idx);
        return r == null ? s.createRow(idx) : r;
    }

    private void clearOldTablesArea(Sheet sheet, int rowStart, int rowEnd) {
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstRow() >= rowStart && region.getFirstRow() <= rowEnd) {
                sheet.removeMergedRegion(i);
            }
        }
        for (int r = rowStart; r <= rowEnd; r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                for (int c = 0; c <= 25; c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) row.removeCell(cell);
                }
            }
        }
    }

    private double calculateSumColumnF(Sheet s) {
        double sum = 0;
        for (int r = 8; r <= 38; r++) sum += getCellValue(s, r, 5);
        return sum;
    }

    private double calculateSumAbsences(Sheet s) {
        double sum = 0;
        for (int r = 8; r <= 38; r++) for (int c = 11; c <= 19; c++) sum += getCellValue(s, r, c);
        return sum;
    }

    private double calculateUsedVacation(Sheet s) {
        double used = 0;
        for (int r = 8; r <= 38; r++) {
            if ("UW".equals(getCellText(s, r, 2))) used += getCellValue(s, r, 11);
        }
        return used;
    }

    private double getCellValue(Sheet s, int r, int c) {
        Row row = s.getRow(r);
        if (row == null) return 0;
        Cell cell = row.getCell(c);
        if (cell != null && cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue() * 24.0;
        return 0;
    }

    private int extractYear(File f) {
        try (FileInputStream i = new FileInputStream(f); Workbook w = new XSSFWorkbook(i)) {
            Matcher m = Pattern.compile("\\d{4}").matcher(getCellText(w.getSheetAt(0), 3, 6));
            if (m.find()) return Integer.parseInt(m.group());
        } catch (Exception e) {
        }
        return 0;
    }

    private String getCellText(Sheet s, int r, int c) {
        Row row = s.getRow(r);
        return (row == null || row.getCell(c) == null) ? "" : row.getCell(c).toString();
    }

    private int detectStartMonth(Workbook wb) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            for (int r = 8; r <= 38; r++) {
                if (parseEtat(getCellText(s, r, 23)) != null) return i + 1;
            }
        }
        return 1;
    }

    private Double parseEtat(String t) {
        try {
            String c = t.replaceAll("[^0-9/.,]", "").replace(",", ".");
            if (c.contains("/")) {
                String[] p = c.split("/");
                return Double.parseDouble(p[0]) / Double.parseDouble(p[1]);
            }
            return Double.parseDouble(c);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isPolishHoliday(LocalDate d) {
        int m = d.getMonthValue(), day = d.getDayOfMonth();
        return (m == 1 && (day == 1 || day == 6)) || (m == 5 && (day == 1 || day == 3)) || (m == 8 && day == 15) || (m == 11 && (day == 1 || day == 11)) || (m == 12 && (day == 25 || day == 26));
    }

    private List<MonthStatus> buildYearMap(Workbook wb, int y, int sd, List<VacationEvent> evs) {
        List<MonthStatus> map = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Sheet sheet = wb.getSheetAt(Math.min(m - 1, wb.getNumberOfSheets() - 1));
            Double fte = null;
            String txt = "1/1";
            for (int r = 8; r <= 39; r++) {
                fte = parseEtat(getCellText(sheet, r, 23));
                if (fte != null) {
                    txt = getCellText(sheet, r, 23);
                    break;
                }
            }
            if (fte == null) {
                Double hF = parseEtat(getCellText(sheet, 7, 23));
                if (hF != null) {
                    fte = hF;
                    txt = getCellText(sheet, 7, 23);
                } else {
                    fte = 1.0;
                }
            }
            int dim = sd;
            for (VacationEvent e : evs) {
                if (e.getDate().getYear() < y || (e.getDate().getYear() == y && e.getDate().getMonthValue() <= m)) dim = e.getNewDimension();
            }
            map.add(new MonthStatus(m, fte, txt, dim));
        }
        return map;
    }

    private static class MonthStatus {
        int monthIndex;
        double fte;
        String fteText;
        int dimension;

        public MonthStatus(int m, double f, String t, int d) {
            this.monthIndex = m;
            this.fte = f;
            this.fteText = t;
            this.dimension = d;
        }
    }
}
