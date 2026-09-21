package com.skiclinic.er;

import com.skiclinic.er.model.Severity;
import com.skiclinic.er.service.ERService;
import com.skiclinic.er.service.TreatmentCapacityPool;
import com.skiclinic.er.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
        mainWing.assignNextPatient().orElseThrow();

        String report = ERReport.render(bays, mainWing, afterHoursWing, 1, 0);

        assertTrue(report.contains("MAIN WING LOBBY"));
        assertTrue(report.contains("AFTER-HOURS WING LOBBY"));
        assertTrue(report.contains("Bays: [X][_]"));
        assertTrue(report.contains("STATUS: HEALTHY"));
    }

    @Test
    void highlightsCapacityAndFlowDiscrepancies() {
        TreatmentCapacityPool bays = new TreatmentCapacityPool(1);
        ERService mainWing = new ERService(bays, clock);
        ERService afterHoursWing = new ERService(bays, clock);
        bays.reserveSlot();
        bays.reserveSlot();

        String report = ERReport.render(bays, mainWing, afterHoursWing, 2, 0);

        assertTrue(report.contains("INVALID: -1 available of 1"));
        assertTrue(report.contains("STATUS: INCONSISTENT"));
        assertTrue(report.contains("! Reported available bays are outside 0..1."));
        assertTrue(report.contains("! Assignments do not equal completions"));
    }
}
