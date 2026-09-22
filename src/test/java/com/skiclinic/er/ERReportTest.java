package com.skiclinic.er;

import com.skiclinic.er.model.Severity;
import com.skiclinic.er.service.ERService;
import com.skiclinic.er.service.TreatmentCapacityPool;
import com.skiclinic.er.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ERReportTest {
    private final MutableClock clock =
            new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));

    @Test
    void rendersBothLobbiesAndAHealthySharedBayPool() {
        TreatmentCapacityPool bays = new TreatmentCapacityPool(2);
        ERService mainWing = new ERService(bays, clock);
        ERService afterHoursWing = new ERService(bays, clock);
        mainWing.admit("M-1", "Main Patient", Severity.MODERATE);
        afterHoursWing.admit("A-1", "After Hours Patient", Severity.MILD);
        String assignedPatientId = mainWing.assignNextPatient().orElseThrow().patient().id();
        mainWing.completeTreatment(assignedPatientId);

        String report = ERReport.render(
                bays, mainWing, afterHoursWing, 1, 1, true);

        assertTrue(report.contains("MAIN WING LOBBY"));
        assertTrue(report.contains("AFTER-HOURS WING LOBBY"));
        assertTrue(report.contains("Bays: [_][_]"));
        assertTrue(report.contains(
                "Assignments: 1 / 2 -> In treatment: 0 -> Completions: 1 / 2"));
        assertTrue(report.contains("not all patients were discharged"));
        assertTrue(report.contains("both wings share the same bays"));
    }

    @Test
    void highlightsCapacityAndFlowDiscrepancies() {
        TreatmentCapacityPool bays = new TreatmentCapacityPool(1);
        ERService mainWing = new ERService(bays, clock);
        ERService afterHoursWing = new ERService(bays, clock);
        mainWing.admit("M-1", "Main Patient", Severity.MODERATE);
        afterHoursWing.admit("A-1", "After Hours Patient", Severity.MILD);
        bays.reserveSlot();
        bays.reserveSlot();

        String report = ERReport.render(
                bays, mainWing, afterHoursWing, 2, 0, true);

        assertTrue(report.contains("INVALID: -1 available of 1"));
        assertTrue(report.contains(
                "Assignments: 2 / 2 -> In treatment: 0 -> Completions: 0 / 2"));
        assertTrue(report.contains("execution cycles exhausted"));
        assertTrue(report.contains("Patients not discharged: 2"));
        assertFalse(report.contains("STATUS:"));
    }

    @Test
    void reportsWhenEveryPatientWasDischarged() {
        TreatmentCapacityPool bays = new TreatmentCapacityPool(1);
        ERService mainWing = new ERService(bays, clock);
        ERService afterHoursWing = new ERService(bays, clock);
        String patientId = mainWing.admit(
                "M-1", "Main Patient", Severity.MODERATE).id();
        mainWing.assignNextPatient().orElseThrow();
        mainWing.completeTreatment(patientId);

        String report = ERReport.render(
                bays, mainWing, afterHoursWing, 1, 1, true);

        assertTrue(report.contains(
                "Assignments: 1 / 1 -> In treatment: 0 -> Completions: 1 / 1"));
        assertTrue(report.contains("Run ended: all 1 patients were discharged."));
    }
}
