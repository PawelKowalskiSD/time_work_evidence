package evidence;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class VacationService {
    // Klasa przechowująca stan urlopu na koniec danego miesiąca
    public static class MonthlySummary {
        public double limitHours;
        public double currentUsed;
        public double totalUsed;
        public double remaining;
        public double overtimeBalance;
    }

    // --- LOGIKA OBLICZEŃ (To już masz) ---
    public static Map<Integer, MonthlySummary> calculateVacationUsage(EvidenceRequest request) {
        Map<Integer, MonthlySummary> summaries = new HashMap<>();
        double totalYearlyHours = calculateTotalYearlyVacationHours(request);
        double totalPool = totalYearlyHours + request.pastDueVacationHours;
        double currentPool = totalPool;
        double overtimePoolMinutes = request.pastDueOvertimeMinutes;

        for (int month = 1; month <= 12; month++) {
            MonthlySummary summary = new MonthlySummary();
            summary.limitHours = totalYearlyHours;
            double usedThisMonthHours = 0;

            LocalDate start = LocalDate.of(request.year, month, 1);
            int len = start.lengthOfMonth();

            for (int d = 1; d <= len; d++) {
                LocalDate date = LocalDate.of(request.year, month, d);
                AbsenceData absence = findAbsence(request.absences, date);

                if (absence != null) {
                    double scheduledHours = getScheduledHours(request.schedulePeriods, date);
                    if (scheduledHours > 0) {
                        if ("UW".equals(absence.getType()) || "UO".equals(absence.getType())) {
                            usedThisMonthHours += scheduledHours;
                            currentPool -= scheduledHours;
                        } else if (absence.isOvertimePickup()) {
                            overtimePoolMinutes -= (scheduledHours * 60);
                        }
                    }
                }
            }
            summary.currentUsed = usedThisMonthHours;
            summary.remaining = currentPool;
            summary.totalUsed = totalPool - currentPool;
            summary.overtimeBalance = overtimePoolMinutes;
            summaries.put(month, summary);
        }
        return summaries;
    }

    // --- NOWA METODA: GENEROWANIE KARTY URLOPOWEJ (Zastępuje generateReport) ---
    public void generateVacationCard(File templateFile, File outputDir, EvidenceRequest request, Map<Integer, MonthlySummary> summaries) throws IOException {
        try (FileInputStream fis = new FileInputStream(templateFile);
             Workbook wb = new XSSFWorkbook(fis)) {

            // Klonujemy arkusz szablonu (zakładamy, że to index 0)
            Sheet sheet = wb.cloneSheet(0);
            wb.setSheetName(wb.getSheetIndex(sheet), String.valueOf(request.year));

            // 1. WYPEŁNIANIE NAGŁÓWKÓW (Dostosuj współrzędne do swojego szablonu!)
            // Przykład: B2=Imie, B3=Rok, B4=Bilans startowy
            setCell(sheet, 1, 1, request.employeeName); // Wiersz 2, Kol B
            setCell(sheet, 2, 1, String.valueOf(request.year)); // Wiersz 3, Kol B

            String startInfo = String.format("Zaległy: %d h, Nadgodziny: %d min", request.pastDueVacationHours, request.pastDueOvertimeMinutes);
            setCell(sheet, 3, 1, startInfo);

            // 2. WYPEŁNIANIE TABELI MIESIĘCY
            // Zakładam, że tabela zaczyna się w wierszu 10 (index 9)
            // Kol A=Miesiąc, B=Limit, C=Zużyte m-c, D=Pozostało, E=Nadgodziny
            int startRow = 9;

            for (int m = 1; m <= 12; m++) {
                MonthlySummary sum = summaries.get(m);
                if (sum == null) continue;

                Row row = sheet.getRow(startRow + m - 1);
                if (row == null) row = sheet.createRow(startRow + m - 1);

                // Kol B: Limit roczny
                createCell(row, 1).setCellValue(sum.limitHours);
                // Kol C: Zużyte w miesiącu
                createCell(row, 2).setCellValue(sum.currentUsed);
                // Kol D: Pozostało
                createCell(row, 3).setCellValue(sum.remaining);
                // Kol E: Saldo nadgodzin (min)
                createCell(row, 4).setCellValue(sum.overtimeBalance);
            }

            // Opcjonalnie usuń arkusz "Wzorcowy" (index 0)
            wb.removeSheetAt(0);

            // Zapisz plik
            String filename = "Karta_Urlopowa_" + request.employeeName.replace(" ", "_") + "_" + request.year + ".xlsx";
            File outFile = new File(outputDir, filename);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                wb.write(fos);
            }
        }
    }

    // --- METODY POMOCNICZE (Prywatne) ---
    private static double calculateTotalYearlyVacationHours(EvidenceRequest request) {
        int baseDays = request.baseVacationDays + (request.isDisabled ? 10 : 0);
        double totalHours = 0;
        if (request.schedulePeriods.size() == 1) {
            return calculateHoursForPeriod(request.schedulePeriods.get(0), baseDays, 12);
        }
        for (SchedulePeriod period : request.schedulePeriods) {
            long months = ChronoUnit.MONTHS.between(period.start.withDayOfMonth(1), period.end.plusDays(1).withDayOfMonth(1));
            if (months == 0) months = 1;
            totalHours += calculateHoursForPeriod(period, baseDays, (int)months);
        }
        return Math.ceil(totalHours);
    }

    private static double calculateHoursForPeriod(SchedulePeriod sp, int baseDays, int monthsDuration) {
        try {
            String[] parts = sp.etat.split("/");
            double numerator = Double.parseDouble(parts[0]);
            double denominator = Double.parseDouble(parts[1]);
            double fraction = numerator / denominator;
            double yearlyDaysForEtat = Math.ceil(baseDays * fraction);
            double daysForPeriod = Math.ceil(yearlyDaysForEtat * (monthsDuration / 12.0));
            return daysForPeriod * 8.0;
        } catch (Exception e) { return 0; }
    }

    private static double getScheduledHours(List<SchedulePeriod> periods, LocalDate date) {
        for (SchedulePeriod sp : periods) {
            if (sp.covers(date)) {
                DailySchedule ds = sp.weeklySchedule.get(date.getDayOfWeek());
                if (ds != null && ds.isActive()) return java.time.Duration.between(ds.getStart(), ds.getEnd()).toMinutes() / 60.0;
            }
        }
        return 0;
    }

    private static AbsenceData findAbsence(List<AbsenceData> absences, LocalDate date) {
        for (AbsenceData a : absences) { if (a.getDate().equals(date)) return a; }
        return null;
    }

    private void setCell(Sheet sheet, int r, int c, String val) {
        Row row = sheet.getRow(r); if (row == null) row = sheet.createRow(r);
        Cell cell = row.getCell(c); if (cell == null) cell = row.createCell(c);
        cell.setCellValue(val);
    }

    private Cell createCell(Row row, int c) {
        Cell cell = row.getCell(c); if (cell == null) cell = row.createCell(c); return cell;
    }
}
