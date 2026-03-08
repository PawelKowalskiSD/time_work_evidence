package evidence.util;

import evidence.model.DailySchedule;
import evidence.model.SchedulePeriod;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Klasa narzędziowa do walidacji harmonogramów pracy.
 * Sprawdza, czy suma godzin w tygodniowym grafiku nie przekracza normy wynikającej z wymiaru etatu.
 */
public class TimeValidator {
    private static final double BASE_FULL_TIME_MINUTES = (37 * 60) + 55; // 37h 55min

    /**
     * Waliduje listę okresów harmonogramu pod kątem nadgodzin.
     *
     * @param periods Lista okresów do sprawdzenia.
     * @return Obiekt {@link ValidationResult} zawierający wyniki walidacji.
     */
    public static ValidationResult validate(List<SchedulePeriod> periods) {
        ValidationResult result = new ValidationResult();

        for (SchedulePeriod period : periods) {
            double requiredMinutes = calculateRequiredMinutes(period.getEtat());
            long scheduledMinutes = calculateScheduledMinutes(period.getWeeklySchedule());

            if (scheduledMinutes > requiredMinutes + 1.0) {
                long overtimeMinutes = (long) (scheduledMinutes - requiredMinutes);
                result.addError(period, requiredMinutes, scheduledMinutes, overtimeMinutes);
            }
        }
        return result;
    }

    /**
     * Oblicza wymaganą liczbę minut pracy w tygodniu na podstawie wymiaru etatu.
     * @param etat Wymiar etatu jako tekst (np. "1/1").
     * @return Wymagana liczba minut.
     */
    private static double calculateRequiredMinutes(String etat) {
        try {
            String[] parts = etat.split("/");
            double numerator = Double.parseDouble(parts[0]);
            double denominator = Double.parseDouble(parts[1]);
            return BASE_FULL_TIME_MINUTES * (numerator / denominator);
        } catch (Exception e) {
            return BASE_FULL_TIME_MINUTES;
        }
    }

    /**
     * Oblicza sumę zaplanowanych minut w tygodniowym harmonogramie.
     * @param weeklySchedule Mapa harmonogramu tygodniowego.
     * @return Suma minut zaplanowanych we wszystkich aktywnych dniach.
     */
    private static long calculateScheduledMinutes(Map<java.time.DayOfWeek, DailySchedule> weeklySchedule) {
        long totalMinutes = 0;
        for (DailySchedule day : weeklySchedule.values()) {
            if (day.isActive()) {
                totalMinutes += Duration.between(day.getStart(), day.getEnd()).toMinutes();
            }
        }
        return totalMinutes;
    }

    /**
     * Klasa przechowująca wyniki walidacji harmonogramu.
     * Gromadzi komunikaty o wykrytych nadgodzinach.
     */
    public static class ValidationResult {
        private final List<String> messages = new ArrayList<>();
        private boolean hasOvertime = false;

        /**
         * Dodaje błąd walidacji do wyniku.
         * @param period Okres, w którym wykryto błąd.
         * @param required Wymagana liczba minut.
         * @param scheduled Zaplanowana liczba minut.
         * @param overtime Liczba minut nadgodzin.
         */
        public void addError(SchedulePeriod period, double required, long scheduled, long overtime) {
            hasOvertime = true;
            String msg = String.format(
                    "Okres: %s - %s (Etat: %s)\n" +
                            "   - Norma: %s\n" +
                            "   - Grafik: %s\n" +
                            "   - NADGODZINY TYGODNIOWE: %s",
                    period.getStart(), period.getEnd(), period.getEtat(),
                    formatMinutes(required),
                    formatMinutes(scheduled),
                    formatMinutes(overtime)
            );
            messages.add(msg);
        }

        /**
         * Sprawdza, czy wykryto jakiekolwiek nadgodziny.
         * @return true, jeśli znaleziono nadgodziny.
         */
        public boolean hasOvertime() { return hasOvertime; }

        /**
         * Zwraca pełny raport z walidacji jako pojedynczy tekst.
         * @return Sformatowany raport.
         */
        public String getReport() { return String.join("\n\n", messages); }

        /**
         * Formatuje liczbę minut do czytelnego formatu "Xh YYmin".
         * @param totalMinutes Liczba minut do sformatowania.
         * @return Sformatowany tekst.
         */
        private String formatMinutes(double totalMinutes) {
            long h = (long) (totalMinutes / 60);
            long m = (long) (totalMinutes % 60);
            return String.format("%dh %02dmin", h, m);
        }
    }
}
