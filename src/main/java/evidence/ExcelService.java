package evidence;

import java.io.IOException;


/**
 * Interfejs serwisu generowania plików.
 * Zgodne z Dependency Inversion - UI zależy od interfejsu, nie konkretnej klasy.
 */
public interface ExcelService {
    void generateEvidence(EvidenceRequest request) throws IOException;
}
