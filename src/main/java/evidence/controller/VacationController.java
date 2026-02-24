package evidence.controller;

import evidence.model.EvidenceRequest;
import java.util.Map;

public interface VacationController {
    Map<Integer, VacationSummary> calculateVacationUsage(EvidenceRequest request);
    
    class VacationSummary {
        public double limitHours;
        public double currentUsed;
        public double totalUsed;
        public double remaining;
        public double overtimeBalance;
        
        public VacationSummary() {}
    }
}
