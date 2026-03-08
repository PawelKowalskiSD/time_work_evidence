package evidence.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * Reprezentuje okres zatrudnienia z określonym harmonogramem i wymiarem etatu.
 * Pracownik może mieć wiele takich okresów w ciągu roku, np. przy zmianie wymiaru etatu.
 */
public class SchedulePeriod {
    private LocalDate start;
    private LocalDate end;
    private String etat;
    private Map<DayOfWeek, DailySchedule> weeklySchedule;

    /**
     * Tworzy nowy okres harmonogramu.
     *
     * @param start Data rozpoczęcia obowiązywania okresu.
     * @param end Data zakończenia obowiązywania okresu.
     * @param etat Wymiar etatu w tym okresie (np. "1/1", "1/2").
     * @param weeklySchedule Mapa definiująca harmonogram pracy dla każdego dnia tygodnia.
     */
    public SchedulePeriod(LocalDate start, LocalDate end, String etat, Map<DayOfWeek, DailySchedule> weeklySchedule) {
        this.start = start;
        this.end = end;
        this.etat = etat;
        this.weeklySchedule = weeklySchedule;
    }

    /**
     * Zwraca datę rozpoczęcia okresu.
     * @return Data rozpoczęcia.
     */
    public LocalDate getStart() { return start; }

    /**
     * Zwraca datę zakończenia okresu.
     * @return Data zakończenia.
     */
    public LocalDate getEnd() { return end; }

    /**
     * Zwraca wymiar etatu.
     * @return Wymiar etatu jako tekst (np. "1/1").
     */
    public String getEtat() { return etat; }

    /**
     * Zwraca tygodniowy harmonogram pracy.
     * @return Mapa harmonogramów dziennych dla każdego dnia tygodnia.
     */
    public Map<DayOfWeek, DailySchedule> getWeeklySchedule() { return weeklySchedule; }

    /**
     * Sprawdza, czy podana data zawiera się w tym okresie.
     *
     * @param date Data do sprawdzenia.
     * @return true, jeśli data znajduje się w przedziale [start, end].
     */
    public boolean covers(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
