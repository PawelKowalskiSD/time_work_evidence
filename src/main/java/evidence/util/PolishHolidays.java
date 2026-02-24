package evidence.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PolishHolidays {
    public static boolean isHoliday(LocalDate date) {
        return getHolidays(date.getYear()).contains(date);
    }

    public static Set<LocalDate> getHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();

        holidays.add(LocalDate.of(year, 1, 1));
        holidays.add(LocalDate.of(year, 1, 6));
        holidays.add(LocalDate.of(year, 5, 1));
        holidays.add(LocalDate.of(year, 5, 3));
        holidays.add(LocalDate.of(year, 8, 15));
        holidays.add(LocalDate.of(year, 11, 1));
        holidays.add(LocalDate.of(year, 11, 11));
        holidays.add(LocalDate.of(year, 12, 25));
        holidays.add(LocalDate.of(year, 12, 26));

        if (year >= 2025) {
            holidays.add(LocalDate.of(year, 12, 24));
        }

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

        holidays.add(easterSunday.plusDays(1));
        holidays.add(easterSunday.plusDays(60));

        return holidays;
    }

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
