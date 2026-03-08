package evidence.controller;

import evidence.model.AbsenceData;
import evidence.model.DailySchedule;
import evidence.model.EvidenceRequest;
import evidence.model.SchedulePeriod;
import evidence.util.PolishHolidays;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ApachePoiExcelService implements EvidenceController {

    private static final Map<String, Integer> ABSENCE_COLUMNS = new HashMap<>();
    static {
        ABSENCE_COLUMNS.put("UW", 11);
        ABSENCE_COLUMNS.put("UM", 12);
        ABSENCE_COLUMNS.put("UO", 13);
        ABSENCE_COLUMNS.put("UB", 14);
        ABSENCE_COLUMNS.put("CH", 15);
        ABSENCE_COLUMNS.put("OP", 16);
        ABSENCE_COLUMNS.put("NU", 17);
        ABSENCE_COLUMNS.put("DEL", 18);
        ABSENCE_COLUMNS.put("NN", 19);
    }

    /**
     * Generuje plik Excel z ewidencją czasu pracy na podstawie dostarczonych danych.
     * Metoda wczytuje szablon, przetwarza każdy miesiąc, wypełniając go danymi o pracy,
     * nieobecnościach i urlopach, a następnie zapisuje wynik do nowego pliku.
     *
     * @param request Obiekt zawierający wszystkie dane potrzebne do wygenerowania ewidencji.
     * @throws IOException W przypadku problemów z odczytem szablonu lub zapisem pliku wynikowego.
     */
    @Override
    public void generateEvidence(EvidenceRequest request) throws IOException {
        try (FileInputStream fis = new FileInputStream(request.getTemplateFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Font fontStandard = workbook.createFont();
            fontStandard.setFontName("Arial");
            fontStandard.setFontHeightInPoints((short) 10);

            Font fontEtatHeader = workbook.createFont();
            fontEtatHeader.setFontName("Arial");
            fontEtatHeader.setFontHeightInPoints((short) 7);
            fontEtatHeader.setBold(true);

            Font fontNote = workbook.createFont();
            fontNote.setFontName("Arial");
            fontNote.setFontHeightInPoints((short) 8);

            Map<LocalDate, AbsenceData> absenceMap = new HashMap<>();
            for (AbsenceData ab : request.getAbsences()) {
                absenceMap.put(ab.getDate(), ab);
            }

            Map<LocalDate, String> etatChangeNotes = calculateEtatChangeNotes(request.getSchedulePeriods());

            VacationService vacationService = new VacationService();
            Map<Integer, VacationController.VacationSummary> summaries = vacationService.calculateVacationUsage(request);

            for (int i = 0; i < 12; i++) {
                if (i >= workbook.getNumberOfSheets()) break;
                Sheet sheet = workbook.getSheetAt(i);
                processMonthSheet(sheet, i + 1, request, fontStandard, fontEtatHeader, fontNote, absenceMap, etatChangeNotes, summaries);
                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            }
            saveWorkbook(workbook, request);
        }
    }

    /**
     * Oblicza i przygotowuje notatki o zmianach wymiaru etatu w ciągu roku.
     * Porównuje kolejne okresy zatrudnienia i tworzy notatkę w dniu, w którym następuje zmiana.
     *
     * @param periods Lista okresów zatrudnienia pracownika.
     * @return Mapa, gdzie kluczem jest data zmiany etatu, a wartością notatka o tej zmianie.
     */
    private Map<LocalDate, String> calculateEtatChangeNotes(List<SchedulePeriod> periods) {
        Map<LocalDate, String> notes = new HashMap<>();
        periods.sort(Comparator.comparing(SchedulePeriod::getStart));

        for (int i = 1; i < periods.size(); i++) {
            SchedulePeriod prev = periods.get(i - 1);
            SchedulePeriod curr = periods.get(i);

            if (!prev.getEtat().equals(curr.getEtat())) {
                notes.put(curr.getStart(), "etat " + curr.getEtat());
            }
        }
        return notes;
    }

    /**
     * Przetwarza pojedynczy arkusz Excel, odpowiadający jednemu miesiącowi.
     * Wypełnia nagłówki, dane dzienne oraz podsumowanie miesiąca.
     *
     * @param sheet Arkusz Excel do przetworzenia.
     * @param month Numer miesiąca (1-12).
     * @param request Główne dane wejściowe.
     * @param fontStandard Standardowa czcionka używana w arkuszu.
     * @param fontEtatHeader Czcionka dla nagłówka etatu.
     * @param fontNote Czcionka dla notatek.
     * @param absenceMap Mapa nieobecności.
     * @param etatChangeNotes Mapa notatek o zmianie etatu.
     * @param summaries Mapa podsumowań urlopowych dla każdego miesiąca.
     */
    private void processMonthSheet(Sheet sheet, int month, EvidenceRequest request,
                                   Font fontStandard, Font fontEtatHeader, Font fontNote,
                                   Map<LocalDate, AbsenceData> absenceMap,
                                   Map<LocalDate, String> etatChangeNotes,
                                   Map<Integer, VacationController.VacationSummary> summaries) {

        LocalDate firstDayOfMonth = LocalDate.of(request.getYear(), month, 1);

        Row headerRow = sheet.getRow(3);
        if (headerRow == null) headerRow = sheet.createRow(3);
        Cell headerCell = headerRow.getCell(6);
        if (headerCell == null) headerCell = headerRow.createCell(6);
        String monthName = firstDayOfMonth.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("pl", "PL"));
        String formattedName = request.getEmployeeName().replaceAll("(?<=\\p{Ll})(?=\\p{Lu})", " ");
        headerCell.setCellValue(formattedName + " - " + monthName + " " + request.getYear());

        SchedulePeriod activePeriod = getScheduleForDate(firstDayOfMonth, request.getSchedulePeriods());
        Row r8 = sheet.getRow(7);
        if (r8 == null) r8 = sheet.createRow(7);
        Cell cEtat = r8.getCell(23);
        if (cEtat == null) cEtat = r8.createCell(23);

        if (activePeriod != null) {
            cEtat.setCellValue("etat " + activePeriod.getEtat());
            applyStyle(cEtat, "General", fontEtatHeader);
        } else {
            cEtat.setBlank();
        }

        Row summaryRowTop = sheet.getRow(4);
        if (summaryRowTop == null) summaryRowTop = sheet.createRow(4);
        Cell abc5Cell = summaryRowTop.getCell(0);
        if (abc5Cell == null) abc5Cell = summaryRowTop.createCell(0);
        abc5Cell.setCellFormula("SUM(F40,L40:T40)");
        applyStyle(abc5Cell, "[h]:mm", fontStandard);

        int daysInMonth = firstDayOfMonth.lengthOfMonth();
        for (int d = 1; d <= 31; d++) {
            Row row = sheet.getRow(8 + d - 1);
            if (row == null) row = sheet.createRow(8 + d - 1);

            if (d > daysInMonth) {
                clearRow(row);
                continue;
            }

            LocalDate currentDate = LocalDate.of(request.getYear(), month, d);
            AbsenceData absence = absenceMap.get(currentDate);
            String changeNote = etatChangeNotes.get(currentDate);

            fillDayRow(row, currentDate, request, fontStandard, fontNote, absence, changeNote);
        }

        VacationController.VacationSummary summary = summaries.get(month);

        if (summary != null) {
            int rowIdx = 42;
            Row summaryRow = sheet.getRow(rowIdx);
            if (summaryRow == null) summaryRow = sheet.createRow(rowIdx);

            Cell infoCell = summaryRow.createCell(1);

            String infoText = String.format(
                    "BILANS URLOPU: Przysługuje: %.0fh | Wykorzystano w tym m-cu: %.0fh | Wykorzystano łącznie: %.0fh | POZOSTAŁO: %.0fh || SALDO NADGODZIN: %.0f min",
                    summary.limitHours + request.getPastDueVacationHours(),
                    summary.currentUsed,
                    summary.totalUsed,
                    summary.remaining,
                    summary.overtimeBalance
            );

            infoCell.setCellValue(infoText);

            CellStyle style = sheet.getWorkbook().createCellStyle();
            Font font = sheet.getWorkbook().createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 9);
            style.setFont(font);
            infoCell.setCellStyle(style);
        }

        Row footerRow = sheet.getRow(39);
        if (footerRow == null) footerRow = sheet.createRow(39);
        Cell sumF = footerRow.getCell(5);
        if (sumF == null) sumF = footerRow.createCell(5);
        sumF.setCellFormula("SUM(F9:F39)");
        applyStyle(sumF, "[h]:mm", fontStandard);

        for (int colIndex = 11; colIndex <= 19; colIndex++) {
            Cell sumCell = footerRow.getCell(colIndex);
            if (sumCell == null) sumCell = footerRow.createCell(colIndex);
            String colLetter = CellReference.convertNumToColString(colIndex);
            sumCell.setCellFormula("SUM(" + colLetter + "9:" + colLetter + "39)");
            applyStyle(sumCell, "[h]:mm", fontStandard);
        }
    }

    /**
     * Znajduje okres harmonogramu, który jest aktywny dla podanej daty.
     *
     * @param date Data, dla której szukany jest harmonogram.
     * @param periods Lista wszystkich okresów harmonogramu.
     * @return Aktywny {@link SchedulePeriod} lub null, jeśli żaden nie pasuje.
     */
    private SchedulePeriod getScheduleForDate(LocalDate date, List<SchedulePeriod> periods) {
        for (SchedulePeriod sp : periods) {
            if (sp.covers(date)) return sp;
        }
        return null;
    }

    /**
     * Wypełnia pojedynczy wiersz w arkuszu, odpowiadający jednemu dniu.
     * Obsługuje dni robocze, weekendy, święta, nieobecności oraz notatki.
     *
     * @param row Wiersz do wypełnienia.
     * @param date Aktualna data.
     * @param request Główne dane wejściowe.
     * @param fontStandard Standardowa czcionka.
     * @param fontNote Czcionka dla notatek.
     * @param absence Dane o nieobecności w tym dniu (lub null).
     * @param changeNote Notatka o zmianie etatu w tym dniu (lub null).
     */
    private void fillDayRow(Row row, LocalDate date, EvidenceRequest request,
                            Font fontStandard, Font fontNote,
                            AbsenceData absence, String changeNote) {

        createCellIfMissing(row, 0).setCellValue(date.getDayOfMonth() + ".");
        createCellIfMissing(row, 1).setCellValue(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("pl", "PL")));

        Cell cType = createCellIfMissing(row, 2);
        Cell cFrom = createCellIfMissing(row, 3);
        Cell cTo = createCellIfMissing(row, 4);
        Cell cHours = createCellIfMissing(row, 5);

        cType.setBlank();
        cFrom.setBlank();
        cTo.setBlank();
        cHours.setBlank();
        applyStyle(cFrom, "HH:mm", fontStandard);
        applyStyle(cTo, "HH:mm", fontStandard);
        applyStyle(cHours, "h:mm", fontStandard);

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int excelRowNum = row.getRowNum() + 1;

        Cell noteCell = createCellIfMissing(row, 23);
        noteCell.setBlank();

        List<String> notes = new ArrayList<>();
        if (absence != null && absence.getNote() != null && !absence.getNote().isEmpty()) {
            notes.add(absence.getNote());
        }
        if (changeNote != null) {
            notes.add(changeNote);
        }

        if (!notes.isEmpty()) {
            noteCell.setCellValue(String.join("; ", notes));
            applyStyle(noteCell, "General", fontNote);
        }

        if (PolishHolidays.isHoliday(date)) {
            cType.setCellValue("SW");
            applyStyle(cType, "General", fontStandard);
            cHours.setCellFormula("MOD(E" + excelRowNum + "-D" + excelRowNum + ",1)");
            return;
        }

        if (dayOfWeek == DayOfWeek.SUNDAY) {
            cType.setCellValue("WN");
            applyStyle(cType, "General", fontStandard);
            return;
        }
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            cType.setCellValue("W5");
            applyStyle(cType, "General", fontStandard);
            return;
        }

        if (absence != null) {
            cType.setCellValue(absence.getType());
            applyStyle(cType, "General", fontStandard);
            cHours.setCellFormula("MOD(E" + excelRowNum + "-D" + excelRowNum + ",1)");

            SchedulePeriod period = getScheduleForDate(date, request.getSchedulePeriods());
            if (period != null) {
                DailySchedule daily = period.getWeeklySchedule().get(date.getDayOfWeek());
                if (daily != null && daily.isActive()) {
                    long mins = ChronoUnit.MINUTES.between(daily.getStart(), daily.getEnd());
                    Integer col = ABSENCE_COLUMNS.get(absence.getType());
                    if (col != null) {
                        Cell ac = createCellIfMissing(row, col);
                        ac.setCellValue(mins / (24.0 * 60.0));
                        applyStyle(ac, "h:mm", fontStandard);
                    }
                }
            }
            return;
        }

        SchedulePeriod period = getScheduleForDate(date, request.getSchedulePeriods());
        if (period == null) return;

        DailySchedule daily = period.getWeeklySchedule().get(dayOfWeek);

        if (daily != null && daily.isActive()) {
            cType.setCellValue("R");
            applyStyle(cType, "General", fontStandard);

            cFrom.setCellValue(daily.getStart().toString());
            cTo.setCellValue(daily.getEnd().toString());
            cHours.setCellFormula("MOD(E" + excelRowNum + "-D" + excelRowNum + ",1)");
        } else {
            cType.setCellValue("WN");
            applyStyle(cType, "General", fontStandard);
        }
    }

    /**
     * Aplikuje styl do komórki, klonując istniejący styl, aby uniknąć modyfikacji
     * współdzielonych obiektów stylów. Ustawia formatowanie, czcionkę i wyrównanie.
     *
     * @param cell Komórka do ostylowania.
     * @param formatStr Format danych (np. "HH:mm", "General").
     * @param font Czcionka do zastosowania.
     */
    private void applyStyle(Cell cell, String formatStr, Font font) {
        Workbook wb = cell.getSheet().getWorkbook();
        CellStyle originalStyle = cell.getCellStyle();
        CellStyle newStyle = wb.createCellStyle();
        newStyle.cloneStyleFrom(originalStyle);
        newStyle.setFont(font);
        newStyle.setAlignment(HorizontalAlignment.CENTER);
        newStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        DataFormat dataFormat = wb.createDataFormat();
        newStyle.setDataFormat(dataFormat.getFormat(formatStr));
        cell.setCellStyle(newStyle);
    }

    /**
     * Czyści zawartość wszystkich komórek w danym wierszu.
     * Używane dla dni, które nie istnieją w danym miesiącu (np. 30 lutego).
     *
     * @param row Wiersz do wyczyszczenia.
     */
    private void clearRow(Row row) {
        for (int i = 0; i <= 23; i++) {
            Cell c = row.getCell(i);
            if (c != null) c.setBlank();
        }
    }

    /**
     * Pobiera komórkę z wiersza. Jeśli komórka nie istnieje, tworzy ją.
     * Zapobiega to błędom NullPointerException.
     *
     * @param row Wiersz, z którego pobierana jest komórka.
     * @param colIndex Indeks kolumny.
     * @return Istniejąca lub nowo utworzona komórka.
     */
    private Cell createCellIfMissing(Row row, int colIndex) {
        Cell c = row.getCell(colIndex);
        if (c == null) c = row.createCell(colIndex);
        return c;
    }

    /**
     * Zapisuje skoroszyt (Workbook) do pliku. Nazwa pliku jest generowana
     * na podstawie imienia, nazwiska pracownika i roku.
     *
     * @param workbook Skoroszyt do zapisania.
     * @param request Dane wejściowe, używane do wygenerowania nazwy pliku.
     * @throws IOException W przypadku problemu z zapisem pliku.
     */
    private void saveWorkbook(Workbook workbook, EvidenceRequest request) throws IOException {
        String formattedName = request.getEmployeeName().replaceAll("(?<=\\p{Ll})(?=\\p{Lu})", " ");
        String filename = "Ewidencja_" + formattedName.replace(" ", "_") + "_" + request.getYear() + ".xlsx";

        File outputFile = new File(request.getOutputDir(), filename);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
        }
    }
}
