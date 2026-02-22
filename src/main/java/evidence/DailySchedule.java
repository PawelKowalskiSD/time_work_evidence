package evidence;


import java.time.LocalTime;

/**
 * Klasa reprezentująca konfigurację pojedynczego dnia tygodnia.
 * Przechowuje informacje: czy dzień jest pracujący i w jakich godzinach.
 */
class DailySchedule {
    private final boolean active;
    private final LocalTime start;
    private final LocalTime end;
    public DailySchedule(boolean active, LocalTime start, LocalTime end) {
        this.active = active; this.start = start; this.end = end;
    }
    public boolean isActive() { return active; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
}
