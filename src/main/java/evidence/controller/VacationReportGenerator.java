package evidence.controller;

import evidence.model.VacationEvent;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VacationReportGenerator {
    private static final double POOL_CONVERSION_FACTOR = 7.0 + (35.0 / 60.0);

    private static class VacationEntry {
        LocalDate start, end;
        double hoursUsed;
        String type;

        VacationEntry(LocalDate s, LocalDate e, double h, String t) {
            start = s;
            end = e;
            hoursUsed = h;
            type = t;
        }
    }

    private static class MonthStatus {
        int monthIndex;
        double fte;
        String fteText;
        int dimension;

        MonthStatus(int m, double f, String t, int d) {
            monthIndex = m;
            fte = f;
            fteText = t;
            dimension = d;
        }
    }

    private static class FTEPeriod {
        int startMonth, endMonth;
        double fte;
        String text;

        FTEPeriod(int s, int e, double f, String t) {
            startMonth = s;
            endMonth = e;
            fte = f;
            text = t;
        }
    }

    public interface ProgressListener {
        void update(String status, int current, int total);
    }

    public void generateReport(List<File> files, File outputDir, int baseYearlyDays, List<VacationEvent> events, ProgressListener listener) {
        try {
            listener.update("Analiza plikow...", 0, 0);
            Map<String, List<File>> employeeFiles = groupFilesByEmployee(files);

            int count = 0;
            for (Map.Entry<String, List<File>> entry : employeeFiles.entrySet()) {
                String employeeName = entry.getKey();
                List<File> empFiles = entry.getValue();

                listener.update("Raport: " + employeeName, count++, employeeFiles.size());
                generateEmployeeReport(employeeName, empFiles, outputDir, baseYearlyDays, events);
            }
            listener.update("Gotowe! " + outputDir.getAbsolutePath(), 100, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateEmployeeReport(String employee, List<File> files, File dir, int baseYearlyDays, List<VacationEvent> events) throws IOException {
        Workbook wb = new XSSFWorkbook();

        CellStyle headerStyle = createStyle(wb, true, true, IndexedColors.GREY_25_PERCENT);
        CellStyle centerStyle = createStyle(wb, false, true, null);
        CellStyle leftStyle = createStyle(wb, false, false, null);
        CellStyle hoursStyle = wb.createCellStyle();
        hoursStyle.cloneStyleFrom(centerStyle);
        hoursStyle.setDataFormat(wb.createDataFormat().getFormat("[h]:mm"));
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.cloneStyleFrom(centerStyle);
        dateStyle.setDataFormat(wb.createDataFormat().getFormat("dd.MM.yyyy"));

        Map<Integer, File> yearMap = new TreeMap<>();
        for (File f : files) {
            int year = extractYearFromFile(f);
            if (year > 0) yearMap.put(year, f);
        }
        if (yearMap.isEmpty()) return;

        Sheet sheet = wb.createSheet("Karta " + employee);
        setupSheetLayout(sheet);
        createMainHeader(sheet, employee, baseYearlyDays, wb);

        int currentRow = 5;
        double currentOverdue = 0;

        int startYear = yearMap.keySet().iterator().next();
        int endYear = ((TreeMap<Integer, File>) yearMap).lastKey();

        for (int year = startYear; year <= endYear; year++) {
            File file = yearMap.get(year);
            List<VacationEntry> vacations = new ArrayList<>();
            List<MonthStatus> yearMapStatus = new ArrayList<>();

            if (file != null) {
                vacations = extractVacationsFromFile(file, year);
                yearMapStatus = buildYearMap(file, year, baseYearlyDays, events);
            } else {
                yearMapStatus = buildEmptyYearMap(year, baseYearlyDays, events);
            }

            double yearPoolHours = calculateComplexPool(yearMapStatus);
            String desc = generateDescription(yearMapStatus);

            currentRow = drawYearSection(sheet, currentRow, year, desc,
                    currentOverdue, yearPoolHours, vacations,
                    headerStyle, centerStyle, leftStyle, dateStyle, hoursStyle);

            double used = vacations.stream().mapToDouble(v -> v.hoursUsed).sum();
            currentOverdue = Math.max(0, (currentOverdue + yearPoolHours) - used);
            currentRow += 2;
        }

        String safeName = employee.replaceAll("[^a-zA-Z0-9 .-]", "_");
        File outFile = new File(dir, "Ewidencja_" + safeName + ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            wb.write(fos);
        }
    }

    private List<MonthStatus> buildYearMap(File file, int year, int startDim, List<VacationEvent> events) {
        List<FTEPeriod> ftePeriods = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook wb = new XSSFWorkbook(fis)) {
            ftePeriods = analyzeYearlyEmployment(wb);
        } catch (Exception e) {
        }
        if (ftePeriods.isEmpty()) ftePeriods.add(new FTEPeriod(1, 12, 1.0, "1/1"));

        List<MonthStatus> map = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            double currentFte = 1.0;
            String currentTxt = "1/1";
            for (FTEPeriod p : ftePeriods) {
                if (m >= p.startMonth && m <= p.endMonth) {
                    currentFte = p.fte;
                    currentTxt = p.text;
                    break;
                }
            }
            int currentDim = startDim;
            for (VacationEvent e : events) {
                if (e.getDate().getYear() < year || (e.getDate().getYear() == year && e.getDate().getMonthValue() <= m)) {
                    currentDim = e.getNewDimension();
                }
            }
            map.add(new MonthStatus(m, currentFte, currentTxt, currentDim));
        }
        return map;
    }

    private List<MonthStatus> buildEmptyYearMap(int year, int startDim, List<VacationEvent> events) {
        List<MonthStatus> map = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            int currentDim = startDim;
            for (VacationEvent e : events) {
                if (e.getDate().getYear() < year || (e.getDate().getYear() == year && e.getDate().getMonthValue() <= m)) {
                    currentDim = e.getNewDimension();
                }
            }
            map.add(new MonthStatus(m, 1.0, "Brak danych (1/1)", currentDim));
        }
        return map;
    }

    private double calculateComplexPool(List<MonthStatus> map) {
        double totalDays = 0;
        int groupStart = 0;
        while (groupStart < map.size()) {
            MonthStatus current = map.get(groupStart);
            int groupLength = 0;
            for (int i = groupStart; i < map.size(); i++) {
                MonthStatus next = map.get(i);
                if (Math.abs(next.fte - current.fte) < 0.001 && next.dimension == current.dimension) groupLength++;
                else break;
            }
            double annualForFte = Math.ceil(current.dimension * current.fte);
            double proportional = Math.ceil(annualForFte * ((double) groupLength / 12.0));
            totalDays += proportional;
            groupStart += groupLength;
        }
        return totalDays * POOL_CONVERSION_FACTOR;
    }

    private List<FTEPeriod> analyzeYearlyEmployment(Workbook wb) {
        List<FTEPeriod> periods = new ArrayList<>();
        FTEPeriod current = null;
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            int month = i + 1;
            Double fte = null;
            String txt = "";
            String hText = getCellText(sheet, 7, 23);
            Double hVal = parseEtat(hText);
            if (hVal != null) {
                fte = hVal;
                txt = hText;
            } else {
                for (int r = 8; r <= 38; r++) {
                    String dText = getCellText(sheet, r, 23);
                    Double dVal = parseEtat(dText);
                    if (dVal != null) {
                        fte = dVal;
                        txt = dText;
                        break;
                    }
                }
            }
            if (fte != null) {
                if (current == null) current = new FTEPeriod(month, month, fte, txt);
                else if (Math.abs(current.fte - fte) < 0.001) current.endMonth = month;
                else {
                    periods.add(current);
                    current = new FTEPeriod(month, month, fte, txt);
                }
            } else if (current != null) current.endMonth = month;
        }
        if (current != null) periods.add(current);
        return periods;
    }

    private int drawYearSection(Sheet sheet, int r, int year, String desc, double overdue, double pool, List<VacationEntry> entries,
                                CellStyle header, CellStyle center, CellStyle left, CellStyle date, CellStyle time) {
        Row title = sheet.createRow(r++);
        createCell(title, 0, "ROK: " + year, header);
        createCell(title, 1, desc, left);
        sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 1, 6));

        Row h = sheet.createRow(r++);
        createCell(h, 0, "Lp.", header);
        createCell(h, 1, "Od", header);
        createCell(h, 2, "Do", header);
        createCell(h, 3, "Rodzaj", header);
        createCell(h, 4, "Zuzyto", header);
        createCell(h, 5, "Saldo", header);
        createCell(h, 6, "Uwagi", header);

        Row bal = sheet.createRow(r++);
        createCell(bal, 0, "-", center);
        createCell(bal, 1, "01.01." + year, center);
        createCell(bal, 2, "-", center);
        createCell(bal, 3, "PULA", center);
        createCell(bal, 4, "-", center);
        createCell(bal, 5, (overdue + pool) / 24.0, time);
        createCell(bal, 6, String.format("Zalegly: %.1f dni, Nowy: %.1f dni", overdue / POOL_CONVERSION_FACTOR, pool / POOL_CONVERSION_FACTOR), left);

        double balance = overdue + pool;
        double trackOverdue = overdue;
        int idx = 1;

        for (VacationEntry v : entries) {
            Row row = sheet.createRow(r++);
            createCell(row, 0, idx++, center);
            createCell(row, 1, v.start, date);
            createCell(row, 2, v.end, date);
            createCell(row, 3, v.type, center);
            createCell(row, 4, v.hoursUsed / 24.0, time);

            balance -= v.hoursUsed;
            createCell(row, 5, balance / 24.0, time);

            String src = "Biezacy";
            if (trackOverdue > 0.01) {
                double taken = Math.min(v.hoursUsed, trackOverdue);
                trackOverdue -= taken;
                src = (taken >= v.hoursUsed - 0.01) ? "Zalegly" : "Mix";
            }
            createCell(row, 6, src, left);
        }
        return r;
    }

    private List<VacationEntry> extractVacationsFromFile(File file, int year) {
        List<VacationEntry> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook wb = new XSSFWorkbook(fis)) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                int month = i + 1;
                for (int r = 8; r <= 38; r++) {
                    String code = getCellText(sheet, r, 2);
                    if ("UW".equals(code) || "UO".equals(code)) {
                        LocalDate d = LocalDate.of(year, month, r - 7);
                        String hStr = getCellText(sheet, r, 11);
                        double h = parseTime(hStr);
                        if (h == 0) h = parseTime(getCellText(sheet, r, 4));
                        if (h > 0.001) list.add(new VacationEntry(d, d, h, code));
                    }
                }
            }
            list.sort(Comparator.comparing(v -> v.start));
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private double parseTime(String s) {
        try {
            return Double.parseDouble(s) * 24.0;
        } catch (Exception e) {
            return 0;
        }
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

    private String getCellText(Sheet s, int r, int c) {
        Row row = s.getRow(r);
        return (row == null || row.getCell(c) == null) ? "" : row.getCell(c).toString();
    }

    private void createCell(Row r, int c, Object v, CellStyle s) {
        Cell cell = r.createCell(c);
        if (v instanceof String) cell.setCellValue((String) v);
        else if (v instanceof Double) cell.setCellValue((Double) v);
        else if (v instanceof Integer) cell.setCellValue((Integer) v);
        else if (v instanceof LocalDate) cell.setCellValue((LocalDate) v);
        cell.setCellStyle(s);
    }

    private int extractYearFromFile(File f) {
        try (FileInputStream i = new FileInputStream(f); Workbook w = new XSSFWorkbook(i)) {
            Matcher m = Pattern.compile("\\d{4}").matcher(getCellText(w.getSheetAt(0), 3, 6));
            if (m.find()) return Integer.parseInt(m.group());
        } catch (Exception e) {
        }
        return 0;
    }

    private String extractNameFromFile(File f) {
        try (FileInputStream i = new FileInputStream(f); Workbook w = new XSSFWorkbook(i)) {
            String h = getCellText(w.getSheetAt(0), 3, 6);
            if (h.contains("-")) return h.split("-")[0].trim();
        } catch (Exception e) {
        }
        return "Pracownik";
    }

    private Map<String, List<File>> groupFilesByEmployee(List<File> fs) {
        Map<String, List<File>> m = new HashMap<>();
        for (File f : fs) m.computeIfAbsent(extractNameFromFile(f), k -> new ArrayList<>()).add(f);
        return m;
    }

    private CellStyle createStyle(Workbook wb, boolean bold, boolean center, IndexedColors bg) {
        CellStyle s = wb.createCellStyle();
        if (bold) {
            Font f = wb.createFont();
            f.setBold(true);
            s.setFont(f);
        }
        s.setAlignment(center ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setWrapText(true);
        if (bg != null) {
            s.setFillForegroundColor(bg.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return s;
    }

    private void createMainHeader(Sheet s, String name, int b, Workbook wb) {
        CellStyle st = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 16);
        st.setFont(f);
        st.setAlignment(HorizontalAlignment.CENTER);
        Row r = s.createRow(0);
        createCell(r, 0, "KARTA EWIDENCJI URLOPOW", st);
        s.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        Row r2 = s.createRow(2);
        createCell(r2, 0, "Pracownik: " + name, wb.createCellStyle());
        s.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));
    }

    private void setupSheetLayout(Sheet s) {
        s.setColumnWidth(0, 1500);
        s.setColumnWidth(1, 3000);
        s.setColumnWidth(2, 3000);
        s.setColumnWidth(3, 2500);
        s.setColumnWidth(4, 3000);
        s.setColumnWidth(5, 3000);
        s.setColumnWidth(6, 10000);
    }

    private String generateDescription(List<MonthStatus> m) {
        StringBuilder sb = new StringBuilder();
        int d = -1;
        double f = -1;
        for (MonthStatus s : m) {
            if (s.dimension != d || Math.abs(s.fte - f) > 0.001) {
                sb.append(String.format("Od m-ca %d: %s, %d dni; ", s.monthIndex, s.fteText, s.dimension));
                d = s.dimension;
                f = s.fte;
            }
        }
        return sb.toString();
    }
}
