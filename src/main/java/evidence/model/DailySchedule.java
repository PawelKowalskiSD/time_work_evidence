package evidence.model;

import java.time.LocalTime;

public class DailySchedule {
    private final boolean active;
    private final LocalTime start;
    private final LocalTime end;

    public DailySchedule(boolean active, LocalTime start, LocalTime end) {
        this.active = active;
        this.start = start;
        this.end = end;
    }

    public boolean isActive() { return active; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
}
