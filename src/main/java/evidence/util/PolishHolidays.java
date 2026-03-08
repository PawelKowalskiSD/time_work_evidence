package evidence.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Klasa narzędziowa do obsługi polskich świąt ustawowo wolnych od pracy.
 * Umożliwia sprawdzanie, czy dany dzień jest świętem, oraz pobieranie listy świąt w danym roku.
 */
public class PolishHolidays {

    /**
     * Sprawdza, czy podana data jest dniem świątecznym w Polsce.
     *
     * @param date Data do sprawdzenia.
     * @return true, jeśli data jest świętem, w przeciwnym razie false.
     */
    public static boolean isHoliday(LocalDate date) {
        return getHolidays(date.getYear()).contains(date);
    }

    /**
     * Zwraca zbiór wszystkich świąt ustawowo wolnych od pracy w podanym roku.
     * Uwzględnia święta stałe oraz ruchome (Wielkanoc, Boże Ciało).
     *
     * @param year Rok, dla którego mają zostać obliczone święta.
     * @return Zbiór obiektów {@link LocalDate} reprezentujących święta.
     */
    public static Set<LocalDate> getHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();

        // Święta stałe
        holidays.add(LocalDate.of(year, 1, 1));   // Nowy Rok
        holidays.add(LocalDate.of(year, 1, 6));   // Trzech Króli
        holidays.add(LocalDate.of(year, 5, 1));   // Święto Pracy
        holidays.add(LocalDate.of(year, 5, 3));   // Święto Konstytucji 3 Maja
        holidays.add(LocalDate.of(year, 8, 15));  // Wniebowzięcie Najświętszej Maryi Panny
        holidays.add(LocalDate.of(year, 11, 1));  // Wszystkich Świętych
        holidays.add(LocalDate.of(year, 11, 11)); // Narodowe Święto Niepodległości
        holidays.add(LocalDate.of(year, 12, 25)); // Boże Narodzenie
        holidays.add(LocalDate.of(year, 12, 26)); // Boże Narodzenie

        // Wigilia wolna od 2025 roku
        if (year >= 2025) {
            holidays.add(LocalDate.of(year, 12, 24));
        }

        // Obliczanie Wielkanocy (algorytm Meeusa/Jonesa/Butchera)
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;

        int p = (h + l - 7 * m + 114) % 31;
        int day = p + 1;
        int month = (h + l - 7 * m + 114) / 31;

        LocalDate easterSunday = LocalDate.of(year, month, day);

        // Święta ruchome zależne od Wielkanocy
        holidays.add(easterSunday.plusDays(1));  // Poniedziałek Wielkanocny
        holidays.add(easterSunday.plusDays(60)); // Boże Ciało

        return holidays;
    }

    /**
     * Zwraca listę świąt, które w danym roku wypadają w sobotę.
     *
     * @param year Rok do sprawdzenia.
     * @return Posortowana lista dat świąt przypadających w sobotę.
     */
    public static List<LocalDate> getHolidaysOnSaturday(int year) {
        Set<LocalDate> holidays = getHolidays(year);
        List<LocalDate> saturdays = new ArrayList<>();
        for (LocalDate date : holidays) {
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
                saturdays.add(date);
            }
        }
        saturdays.sort(LocalDate::compareTo);
        return saturdays;
    }
}
