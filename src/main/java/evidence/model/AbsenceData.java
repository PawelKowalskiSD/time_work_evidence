package evidence.model;

import java.time.LocalDate;

/**
 * Reprezentuje pojedynczą nieobecność pracownika w określonym dniu.
 * Przechowuje typ nieobecności, datę oraz opcjonalną notatkę.
 */
public class AbsenceData {
    private final String type;
    private final LocalDate date;
    private final String note;
    private boolean overtimePickup;

    /**
     * Tworzy nową instancję danych o nieobecności.
     *
     * @param type Kod nieobecności (np. "UW" dla urlopu wypoczynkowego).
     * @param date Data nieobecności.
     * @param note Dodatkowa notatka związana z nieobecnością.
     */
    public AbsenceData(String type, LocalDate date, String note) {
        this.type = type;
        this.date = date;
        this.note = note;
    }

    /**
     * Zwraca typ nieobecności.
     * @return Kod typu nieobecności.
     */
    public String getType() { return type; }

    /**
     * Zwraca datę nieobecności.
     * @return Data nieobecności.
     */
    public LocalDate getDate() { return date; }

    /**
     * Zwraca notatkę.
     * @return Notatka lub null, jeśli brak.
     */
    public String getNote() { return note; }

    /**
     * Sprawdza, czy nieobecność jest odbiorem za nadgodziny.
     * @return true, jeśli jest to odbiór nadgodzin.
     */
    public boolean isOvertimePickup() {
        return overtimePickup;
    }

    /**
     * Ustawia flagę odbioru za nadgodziny.
     * @param overtimePickup true, aby oznaczyć jako odbiór nadgodzin.
     */
    public void setOvertimePickup(boolean overtimePickup) {
        this.overtimePickup = overtimePickup;
    }
}
