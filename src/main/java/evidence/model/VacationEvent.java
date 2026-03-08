package evidence.model;

import java.time.LocalDate;

/**
 * Reprezentuje zdarzenie mające wpływ na wymiar urlopu pracownika.
 * Przykładem takiego zdarzenia jest osiągnięcie określonego stażu pracy,
 * co skutkuje zwiększeniem rocznego wymiaru urlopu.
 */
public class VacationEvent {
    private LocalDate date;
    private int newDimension;
    private String description;

    /**
     * Tworzy nowe zdarzenie urlopowe.
     *
     * @param date Data, od której obowiązuje zmiana.
     * @param newDimension Nowy roczny wymiar urlopu w dniach (np. 26).
     * @param description Opis zdarzenia (np. "10 lat stażu pracy").
     */
    public VacationEvent(LocalDate date, int newDimension, String description) {
        this.date = date;
        this.newDimension = newDimension;
        this.description = description;
    }

    /**
     * Zwraca datę, od której obowiązuje zmiana.
     * @return Data zdarzenia.
     */
    public LocalDate getDate() { return date; }

    /**
     * Zwraca nowy wymiar urlopu.
     * @return Nowy roczny wymiar urlopu w dniach.
     */
    public int getNewDimension() { return newDimension; }

    /**
     * Zwraca opis zdarzenia.
     * @return Tekstowy opis zdarzenia.
     */
    public String getDescription() { return description; }

    /**
     * Zwraca tekstową reprezentację obiektu.
     * @return Sformatowany ciąg znaków opisujący zdarzenie.
     */
    @Override
    public String toString() {
        return date + ": " + newDimension + " dni (" + description + ")";
    }
}
