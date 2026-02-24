package evidence.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class SchedulePeriod {
    private LocalDate start;
    private LocalDate end;
    private String etat;
    private Map<DayOfWeek, DailySchedule> weeklySchedule;

    public SchedulePeriod(LocalDate start, LocalDate end, String etat, Map<DayOfWeek, DailySchedule> weeklySchedule) {
        this.start = start;
        this.end = end;
        this.etat = etat;
        this.weeklySchedule = weeklySchedule;
    }

    public LocalDate getStart() { return start; }
    public LocalDate getEnd() { return end; }
    public String getEtat() { return etat; }
    public Map<DayOfWeek, DailySchedule> getWeeklySchedule() { return weeklySchedule; }

    public boolean covers(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
