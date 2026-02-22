package evidence;

import java.util.HashMap;
import java.util.Map;

public class VacationUsage {
    int year;
    // Mapa: Numer miesiąca (1-12) -> Liczba dni urlopu
    Map<Integer, Integer> daysPerMonth = new HashMap<>();

    public VacationUsage(int year) {
        this.year = year;
        for (int i = 1; i <= 12; i++) daysPerMonth.put(i, 0);
    }

    public void addDay(int month) {
        daysPerMonth.put(month, daysPerMonth.get(month) + 1);
    }

    public int getTotalUsed() {
        return daysPerMonth.values().stream().mapToInt(Integer::intValue).sum();
    }
}
