package evidence.model;

import java.time.LocalTime;

/**
 * Reprezentuje dzienny harmonogram pracy.
 * Określa, czy dany dzień jest dniem pracującym oraz godziny rozpoczęcia i zakończenia pracy.
 */
public class DailySchedule {
    private final boolean active;
    private final LocalTime start;
    private final LocalTime end;

    /**
     * Tworzy nowy dzienny harmonogram.
     *
     * @param active true, jeśli jest to dzień pracujący, w przeciwnym razie false.
     * @param start Godzina rozpoczęcia pracy (ignorowane, jeśli active=false).
     * @param end Godzina zakończenia pracy (ignorowane, jeśli active=false).
     */
    public DailySchedule(boolean active, LocalTime start, LocalTime end) {
        this.active = active;
        this.start = start;
        this.end = end;
    }

    /**
     * Sprawdza, czy jest to dzień pracujący.
     * @return true, jeśli dzień jest pracujący.
     */
    public boolean isActive() { return active; }

    /**
     * Zwraca godzinę rozpoczęcia pracy.
     * @return Godzina rozpoczęcia pracy.
     */
    public LocalTime getStart() { return start; }

    /**
     * Zwraca godzinę zakończenia pracy.
     * @return Godzina zakończenia pracy.
     */
    public LocalTime getEnd() { return end; }
}
