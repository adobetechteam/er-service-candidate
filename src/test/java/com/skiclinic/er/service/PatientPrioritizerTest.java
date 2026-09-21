package com.skiclinic.er.service;

import com.skiclinic.er.model.Patient;
import com.skiclinic.er.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatientPrioritizerTest {
    private final PatientPrioritizer prioritizer = new PatientPrioritizer();

    @Test
    void selectsTheEarlierArrivalWhenPatientsAreOtherwiseEquivalent() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        Patient earlier = new Patient("P-1", "Earlier", Severity.MODERATE, now.minusSeconds(20));
        Patient later = new Patient("P-2", "Later", Severity.MODERATE, now.minusSeconds(10));

        Patient selected = prioritizer.selectNext(List.of(later, earlier), now).orElseThrow();

        assertEquals(earlier, selected);
    }

    @Test
    void usesThePatientIdAsTheFinalTieBreaker() {
        Instant arrival = Instant.parse("2026-01-01T11:00:00Z");
        Patient second = new Patient("P-2", "Second", Severity.MILD, arrival);
        Patient first = new Patient("P-1", "First", Severity.MILD, arrival);

        Patient selected = prioritizer.selectNext(List.of(second, first), arrival).orElseThrow();

        assertEquals(first, selected);
    }
}

