package evidence;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class SchedulePeriod {
    LocalDate start;
    LocalDate end;
    String etat; // np. "1/1", "1/2"
    Map<DayOfWeek, DailySchedule> weeklySchedule;

    public SchedulePeriod(LocalDate start, LocalDate end, String etat, Map<DayOfWeek, DailySchedule> weeklySchedule) {
        this.start = start;
        this.end = end;
        this.etat = etat;
        this.weeklySchedule = weeklySchedule;
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
