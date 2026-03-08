package evidence.model;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Agreguje wszystkie dane wejściowe potrzebne do wygenerowania ewidencji czasu pracy.
 * Obiekt tej klasy jest przekazywany do kontrolerów w celu przetworzenia.
 */
public class EvidenceRequest {
    private File templateFile;
    private String employeeName;
    private int year;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<SchedulePeriod> schedulePeriods;
    private List<AbsenceData> absences;
    private boolean isDisabled;
    private int baseVacationDays;
    private File outputDir;
    private int pastDueVacationHours;
    private int pastDueOvertimeMinutes;

    /**
     * Tworzy nowe żądanie wygenerowania ewidencji.
     *
     * @param templateFile Plik szablonu Excel.
     * @param employeeName Imię i nazwisko pracownika.
     * @param year Rok, którego dotyczy ewidencja.
     * @param startDate Data rozpoczęcia okresu ewidencji.
     * @param endDate Data zakończenia okresu ewidencji.
     * @param schedulePeriods Lista okresów harmonogramu pracownika.
     * @param absences Lista nieobecności pracownika.
     * @param isDisabled Czy pracownik posiada orzeczenie o niepełnosprawności.
     * @param baseVacationDays Bazowy wymiar urlopu (20 lub 26 dni).
     * @param outputDir Katalog docelowy dla wygenerowanego pliku.
     * @param pastDueVacationDays Liczba dni urlopu zaległego z poprzedniego roku.
     * @param pastDueOvertimeHours Liczba godzin nadgodzin z poprzedniego roku.
     */
    public EvidenceRequest(File templateFile, String employeeName, int year,
                           LocalDate startDate, LocalDate endDate,
                           List<SchedulePeriod> schedulePeriods,
                           List<AbsenceData> absences,
                           boolean isDisabled, int baseVacationDays,
                           File outputDir,
                           int pastDueVacationDays,
                           int pastDueOvertimeHours
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
        this.pastDueVacationHours = pastDueVacationDays * 8;
        this.pastDueOvertimeMinutes = pastDueOvertimeHours * 60;
    }

    public File getTemplateFile() { return templateFile; }
    public String getEmployeeName() { return employeeName; }
    public int getYear() { return year; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<SchedulePeriod> getSchedulePeriods() { return schedulePeriods; }
    public List<AbsenceData> getAbsences() { return absences; }
    public boolean isDisabled() { return isDisabled; }
    public int getBaseVacationDays() { return baseVacationDays; }
    public File getOutputDir() { return outputDir; }
    public int getPastDueVacationHours() { return pastDueVacationHours; }
    public int getPastDueOvertimeMinutes() { return pastDueOvertimeMinutes; }
}
