package evidence.model;

import java.time.LocalDate;

public class VacationEvent {
    private LocalDate date;
    private int newDimension;
    private String description;

    public VacationEvent(LocalDate date, int newDimension, String description) {
        this.date = date;
        this.newDimension = newDimension;
        this.description = description;
    }

    public LocalDate getDate() { return date; }
    public int getNewDimension() { return newDimension; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return date + ": " + newDimension + " dni (" + description + ")";
    }
}
