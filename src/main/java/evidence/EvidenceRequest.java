package evidence;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

class EvidenceRequest {
    File templateFile;
    String employeeName;
    int year;
    LocalDate startDate;
    LocalDate endDate;
    List<SchedulePeriod> schedulePeriods;
    List<AbsenceData> absences;
    boolean isDisabled;
    int baseVacationDays;
    File outputDir;

    int pastDueVacationHours; // Zaległy urlop w godzinach (łatwiej liczyć)
    int pastDueOvertimeMinutes; // Nadgodziny z zeszłego roku (minuty)

    public EvidenceRequest(File templateFile, String employeeName, int year,
                           LocalDate startDate, LocalDate endDate,
                           List<SchedulePeriod> schedulePeriods,
                           List<AbsenceData> absences,
                           boolean isDisabled, int baseVacationDays,
                           File outputDir,
                           int pastDueVacationDays, // Podajemy w dniach (dla wygody UI)
                           int pastDueOvertimeHours // Podajemy w godzinach
    ) {
        this.templateFile = templateFile;
        this.employeeName = employeeName;
        this.year = year;
        this.startDate = startDate;
        this.endDate = endDate;
        this.schedulePeriods = schedulePeriods;
        this.absences = absences;
        this.isDisabled = isDisabled;
        this.baseVacationDays = baseVacationDays;
        this.outputDir = outputDir;

        // Konwersja na jednostki bazowe (dni -> godziny, godziny -> minuty)
        // Zakładamy standardowo 1 dzień zaległego = 8h (nawet przy niepełnym etacie w zeszłym roku rozlicza się to zazwyczaj do 8h)
        this.pastDueVacationHours = pastDueVacationDays * 8;
        this.pastDueOvertimeMinutes = pastDueOvertimeHours * 60;
    }
}
