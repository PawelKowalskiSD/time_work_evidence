package evidence.controller;

import evidence.model.EvidenceRequest;
import java.io.IOException;

/**
 * Interfejs dla kontrolerów generujących ewidencję czasu pracy.
 * Definiuje podstawową operację generowania dokumentu ewidencji.
 */
public interface EvidenceController {
    /**
     * Generuje dokument ewidencji czasu pracy na podstawie dostarczonych danych.
     *
     * @param request Obiekt zawierający wszystkie dane potrzebne do wygenerowania ewidencji.
     * @throws IOException W przypadku problemów z tworzeniem lub zapisem pliku.
     */
    void generateEvidence(EvidenceRequest request) throws IOException;
}
