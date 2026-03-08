package evidence.controller;

import evidence.model.AbsenceData;
import evidence.model.DailySchedule;
import evidence.model.EvidenceRequest;
import evidence.model.SchedulePeriod;
import evidence.util.PolishHolidays;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Serwis do obliczania wykorzystania urlopu oraz bilansu nadgodzin.
 * Implementuje logikę związaną z pulą urlopową pracownika.
 */
public class VacationService implements VacationController {

    /**
     * Oblicza wykorzystanie urlopu i stan nadgodzin dla każdego miesiąca w roku.
     * Tworzy podsumowanie dla każdego miesiąca, uwzględniając urlop zaległy,
     * roczną pulę urlopową oraz odbiór nadgodzin.
     *
     * @param request Obiekt zawierający dane pracownika, jego harmonogramy i nieobecności.
     * @return Mapa, gdzie kluczem jest numer miesiąca (1-12), a wartością obiekt {@link VacationSummary}
     *         zawierający szczegółowe podsumowanie dla tego miesiąca.
     */
    @Override
    public Map<Integer, VacationSummary> calculateVacationUsage(EvidenceRequest request) {
        Map<Integer, VacationSummary> summaries = new HashMap<>();
        double totalYearlyHours = calculateTotalYearlyVacationHours(request);
        double totalPool = totalYearlyHours + request.getPastDueVacationHours();
        double currentPool = totalPool;
        double overtimePoolMinutes = request.getPastDueOvertimeMinutes();

        for (int month = 1; month <= 12; month++) {
            VacationSummary summary = new VacationSummary();
            summary.limitHours = totalYearlyHours;
            double usedThisMonthHours = 0;

            LocalDate start = LocalDate.of(request.getYear(), month, 1);
            int len = start.lengthOfMonth();

            for (int d = 1; d <= len; d++) {
                LocalDate date = LocalDate.of(request.getYear(), month, d);
                AbsenceData absence = findAbsence(request.getAbsences(), date);

                if (absence != null) {
                    double scheduledHours = getScheduledHours(request.getSchedulePeriods(), date);
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

    /**
     * Oblicza całkowitą roczną pulę urlopową w godzinach na podstawie wymiaru etatu i okresów zatrudnienia.
     * Uwzględnia dodatkowe dni urlopu dla osób niepełnosprawnych.
     *
     * @param request Dane wejściowe zawierające informacje o pracowniku i jego harmonogramach.
     * @return Całkowita liczba godzin urlopu na dany rok.
     */
    private double calculateTotalYearlyVacationHours(EvidenceRequest request) {
        int baseDays = request.getBaseVacationDays() + (request.isDisabled() ? 10 : 0);
        double totalHours = 0;
        if (request.getSchedulePeriods().size() == 1) {
            return calculateHoursForPeriod(request.getSchedulePeriods().get(0), baseDays, 12);
        }
        for (SchedulePeriod period : request.getSchedulePeriods()) {
            long months = ChronoUnit.MONTHS.between(period.getStart().withDayOfMonth(1), period.getEnd().plusDays(1).withDayOfMonth(1));
            if (months == 0) months = 1;
            totalHours += calculateHoursForPeriod(period, baseDays, (int) months);
        }
        return Math.ceil(totalHours);
    }

    /**
     * Oblicza liczbę godzin urlopu dla danego okresu zatrudnienia, wymiaru etatu i czasu trwania.
     *
     * @param sp Okres harmonogramu z określonym wymiarem etatu.
     * @param baseDays Bazowy wymiar urlopu w dniach (np. 20 lub 26).
     * @param monthsDuration Czas trwania okresu w miesiącach.
     * @return Liczba godzin urlopu proporcjonalna do etatu i czasu trwania okresu.
     */
    private double calculateHoursForPeriod(SchedulePeriod sp, int baseDays, int monthsDuration) {
        try {
            String[] parts = sp.getEtat().split("/");
            double numerator = Double.parseDouble(parts[0]);
            double denominator = Double.parseDouble(parts[1]);
            double fraction = numerator / denominator;
            double yearlyDaysForEtat = Math.ceil(baseDays * fraction);
            double daysForPeriod = Math.ceil(yearlyDaysForEtat * ((double) monthsDuration / 12.0));
            return daysForPeriod * 8.0;
        } catch (Exception e) { return 0; }
    }

    /**
     * Zwraca liczbę zaplanowanych godzin pracy dla danego dnia na podstawie harmonogramu.
     *
     * @param periods Lista okresów harmonogramu pracownika.
     * @param date Data, dla której mają być sprawdzone godziny.
     * @return Liczba zaplanowanych godzin pracy lub 0, jeśli dzień jest wolny.
     */
    private double getScheduledHours(List<SchedulePeriod> periods, LocalDate date) {
        for (SchedulePeriod sp : periods) {
            if (sp.covers(date)) {
                DailySchedule ds = sp.getWeeklySchedule().get(date.getDayOfWeek());
                if (ds != null && ds.isActive()) {
                    return java.time.Duration.between(ds.getStart(), ds.getEnd()).toMinutes() / 60.0;
                }
            }
        }
        return 0;
    }

    /**
     * Wyszukuje nieobecność dla podanej daty na liście nieobecności.
     *
     * @param absences Lista wszystkich nieobecności pracownika.
     * @param date Data, dla której szukana jest nieobecność.
     * @return Obiekt {@link AbsenceData} jeśli znaleziono, w przeciwnym razie null.
     */
    private AbsenceData findAbsence(List<AbsenceData> absences, LocalDate date) {
        for (AbsenceData a : absences) {
            if (a.getDate().equals(date)) return a;
        }
        return null;
    }
}
