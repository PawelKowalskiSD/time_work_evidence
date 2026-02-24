package evidence.controller;

import evidence.model.EvidenceRequest;
import java.io.IOException;

public interface EvidenceController {
    void generateEvidence(EvidenceRequest request) throws IOException;
}
