package evidence.util;

import evidence.model.DailySchedule;
import evidence.model.SchedulePeriod;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TimeValidator {
    private static final double BASE_FULL_TIME_MINUTES = (37 * 60) + 55;

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

    private static long calculateScheduledMinutes(Map<java.time.DayOfWeek, DailySchedule> weeklySchedule) {
        long totalMinutes = 0;
        for (DailySchedule day : weeklySchedule.values()) {
            if (day.isActive()) {
                totalMinutes += Duration.between(day.getStart(), day.getEnd()).toMinutes();
            }
        }
        return totalMinutes;
    }

    public static class ValidationResult {
        private final List<String> messages = new ArrayList<>();
        private boolean hasOvertime = false;

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

        public boolean hasOvertime() { return hasOvertime; }
        public String getReport() { return String.join("\n\n", messages); }

        private String formatMinutes(double totalMinutes) {
            long h = (long) (totalMinutes / 60);
            long m = (long) (totalMinutes % 60);
            return String.format("%dh %02dmin", h, m);
        }
    }
}
