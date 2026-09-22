package com.skiclinic.er;

import com.skiclinic.er.model.PatientStatus;
import com.skiclinic.er.service.ERService;
import com.skiclinic.er.service.TreatmentCapacityPool;

public final class ERReport {
    private static final int COLUMN_WIDTH = 38;
    private static final String FULL_BORDER =
            "+==============================================================================+";
    private static final String WING_BORDER =
            "+--------------------------------------+--------------------------------------+";

    public static String render(TreatmentCapacityPool bays,
                                ERService mainWing,
                                ERService afterHoursWing,
                                int assignments,
                                int completions,
                                boolean workersFinished) {
        WingStats main = WingStats.from(mainWing);
        WingStats afterHours = WingStats.from(afterHoursWing);
        long totalPatients = main.total() + afterHours.total();
        long actualInTreatment = main.inTreatment + afterHours.inTreatment;
        long actualDischarged = main.discharged + afterHours.discharged;
        int reportedAvailable = bays.availableSlots();
        long expectedAvailable = bays.totalCapacity() - actualInTreatment;

        StringBuilder report = new StringBuilder();
        report.append(FULL_BORDER).append('\n');
        report.append(fullRow("SKI TOWN ER - SHARED CAPACITY LOAD REPORT")).append('\n');
        report.append(FULL_BORDER).append('\n');
        report.append(wingRow("MAIN WING LOBBY", "AFTER-HOURS WING LOBBY")).append('\n');
        report.append(wingRow("Waiting:      " + main.waiting,
                "Waiting:      " + afterHours.waiting)).append('\n');
        report.append(wingRow("In treatment: " + main.inTreatment,
                "In treatment: " + afterHours.inTreatment)).append('\n');
        report.append(wingRow("Discharged:   " + main.discharged,
                "Discharged:   " + afterHours.discharged)).append('\n');
        report.append(WING_BORDER).append('\n');
        report.append(fullRow("\\              both wings share the same bays               /"))
                .append('\n');
        report.append(FULL_BORDER).append('\n');
        report.append(fullRow("SHARED TREATMENT BAYS")).append('\n');
        report.append(fullRow("Bays: " + renderBays(bays.totalCapacity(), reportedAvailable))).append('\n');
        report.append(fullRow("Total: " + bays.totalCapacity()
                + " | Reported available: " + reportedAvailable
                + " | Patient states imply available: " + expectedAvailable)).append('\n');
        report.append(FULL_BORDER).append('\n');
        report.append(fullRow("FLOW TOTALS")).append('\n');
        report.append(fullRow("Assignments: " + assignments + " / " + totalPatients
                + " -> In treatment: " + actualInTreatment
                + " -> Completions: " + completions + " / " + totalPatients)).append('\n');
        report.append(FULL_BORDER).append('\n');
        report.append(fullRow("RUN COMPLETION")).append('\n');
        if (actualDischarged == totalPatients) {
            report.append(fullRow("Run ended: all " + totalPatients + " patients were discharged."))
                    .append('\n');
        } else if (workersFinished) {
            report.append(fullRow(
                    "Run ended: execution cycles exhausted; not all patients were discharged."))
                    .append('\n');
            report.append(fullRow("Patients not discharged: " + (totalPatients - actualDischarged)))
                    .append('\n');
        } else {
            report.append(fullRow(
                    "Run ended: worker timeout; not all patients were discharged.")).append('\n');
            report.append(fullRow("Patients not discharged: " + (totalPatients - actualDischarged)))
                    .append('\n');
        }
        report.append(FULL_BORDER).append('\n');
        return report.toString();
    }

    private static String renderBays(int total, int available) {
        if (available < 0 || available > total) {
            return "[INVALID: " + available + " available of " + total + "]";
        }

        int occupied = total - available;
        StringBuilder bays = new StringBuilder();
        for (int i = 0; i < total; i++) {
            bays.append(i < occupied ? "[X]" : "[_]");
        }
        bays.append("  X=occupied, _=available");
        return bays.toString();
    }

    private static String fullRow(String value) {
        return "|" + fit(value, (COLUMN_WIDTH * 2) + 1) + "|";
    }

    private static String wingRow(String left, String right) {
        return "|" + fit(left, COLUMN_WIDTH) + "|" + fit(right, COLUMN_WIDTH) + "|";
    }

    private static String fit(String value, int width) {
        String clipped = value.length() > width ? value.substring(0, width) : value;
        return String.format(" %-" + (width - 1) + "s", clipped);
    }

    private static final class WingStats {
        private final long waiting;
        private final long inTreatment;
        private final long discharged;

        private WingStats(long waiting, long inTreatment, long discharged) {
            this.waiting = waiting;
            this.inTreatment = inTreatment;
            this.discharged = discharged;
        }

        private long total() {
            return waiting + inTreatment + discharged;
        }

        private static WingStats from(ERService wing) {
            long waiting = wing.patients().stream()
                    .filter(patient -> patient.status() == PatientStatus.WAITING)
                    .count();
            long inTreatment = wing.patients().stream()
                    .filter(patient -> patient.status() == PatientStatus.IN_TREATMENT)
                    .count();
            long discharged = wing.patients().stream()
                    .filter(patient -> patient.status() == PatientStatus.DISCHARGED)
                    .count();
            return new WingStats(waiting, inTreatment, discharged);
        }
    }

    private ERReport() {
    }
}
