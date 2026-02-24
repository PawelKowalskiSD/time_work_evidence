package evidence.model;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

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
