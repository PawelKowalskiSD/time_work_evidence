package evidence.controller;

import evidence.model.EvidenceRequest;
import java.util.Map;

/**
 * Interfejs dla kontrolerów obliczających wykorzystanie urlopu.
 */
public interface VacationController {

    /**
     * Oblicza wykorzystanie urlopu dla każdego miesiąca na podstawie dostarczonych danych.
     *
     * @param request Obiekt zawierający dane pracownika, harmonogramy i nieobecności.
     * @return Mapa podsumowań urlopowych dla każdego miesiąca (klucz: numer miesiąca).
     */
    Map<Integer, VacationSummary> calculateVacationUsage(EvidenceRequest request);

    /**
     * Klasa przechowująca podsumowanie danych urlopowych i nadgodzin dla danego okresu (zazwyczaj miesiąca).
     */
    class VacationSummary {
        /** Całkowita roczna pula urlopowa w godzinach. */
        public double limitHours;
        /** Godziny urlopu wykorzystane w bieżącym okresie. */
        public double currentUsed;
        /** Łączne godziny urlopu wykorzystane od początku roku. */
        public double totalUsed;
        /** Godziny urlopu pozostałe do wykorzystania. */
        public double remaining;
        /** Aktualny bilans nadgodzin w minutach. */
        public double overtimeBalance;

        public VacationSummary() {}
    }
}
