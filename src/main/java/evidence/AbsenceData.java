package evidence;

import java.time.LocalDate;

public class AbsenceData {
    private final String type; // np. UW, CH
    private final LocalDate date;
    private final String note;
    boolean overtimePickup;

    public AbsenceData(String type, LocalDate date, String note) {
        this.type = type;
        this.date = date;
        this.note = note;
    }

    public String getType() { return type; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }

    public boolean isOvertimePickup() {
        return overtimePickup;
    }

    public void setOvertimePickup(boolean overtimePickup) {
        this.overtimePickup = overtimePickup;
    }
}
