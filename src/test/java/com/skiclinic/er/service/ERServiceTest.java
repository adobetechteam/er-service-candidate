package com.skiclinic.er.service;

import com.skiclinic.er.model.PatientStatus;
import com.skiclinic.er.model.Severity;
import com.skiclinic.er.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ERServiceTest {
    private MutableClock clock;
    private ERService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        service = new ERService(new TreatmentCapacityPool(1), clock);
    }

    @Test
    void doesNotExceedTreatmentCapacitySequentially() {
        service.admit("P-1", "First", Severity.MODERATE);
        clock.advance(Duration.ofMinutes(1));
        service.admit("P-2", "Second", Severity.MODERATE);

        assertTrue(service.assignNextPatient().isPresent());
        assertFalse(service.assignNextPatient().isPresent());
        assertEquals(0, service.availableTreatmentSlots());
        assertEquals(1, service.waitingPatientCount());
    }

    @Test
    void completingTreatmentReleasesOneSlot() {
        service.admit("P-1", "First", Severity.MODERATE);
        TreatmentAssignment assignment = service.assignNextPatient().orElseThrow();

        assertTrue(service.completeTreatment(assignment.patient().id()));
        assertFalse(service.completeTreatment(assignment.patient().id()));
        assertEquals(PatientStatus.DISCHARGED, assignment.patient().status());
        assertEquals(1, service.availableTreatmentSlots());
    }

    @Test
    void treatmentCapacityIsSharedAcrossWings() {
        TreatmentCapacityPool sharedPool = new TreatmentCapacityPool(1);
        ERService mainWing = new ERService(sharedPool, clock);
        ERService afterHoursWing = new ERService(sharedPool, clock);

        mainWing.admit("P-1", "Main Wing Patient", Severity.MODERATE);
        afterHoursWing.admit("P-2", "After Hours Patient", Severity.MODERATE);

        assertTrue(mainWing.assignNextPatient().isPresent());
        assertFalse(afterHoursWing.assignNextPatient().isPresent());
        assertEquals(0, sharedPool.availableSlots());
    }
}
