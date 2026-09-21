package com.skiclinic.er;

import com.skiclinic.er.model.PatientStatus;
import com.skiclinic.er.service.ERService;
import com.skiclinic.er.service.TreatmentCapacityPool;

import java.util.ArrayList;
import java.util.List;

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
                                int completions) {
        WingStats main = WingStats.from(mainWing);
        WingStats afterHours = WingStats.from(afterHoursWing);
        long actualInTreatment = main.inTreatment + afterHours.inTreatment;
        int reportedAvailable = bays.availableSlots();
        long expectedAvailable = bays.totalCapacity() - actualInTreatment;

        List<String> warnings = new ArrayList<>();
        if (reportedAvailable < 0 || reportedAvailable > bays.totalCapacity()) {
            warnings.add("Reported available bays are outside 0.." + bays.totalCapacity() + ".");
        }
        if (reportedAvailable != expectedAvailable) {
            warnings.add("Bay pool reports " + reportedAvailable + " available; patient states imply "
                    + expectedAvailable + ".");
        }
        if (assignments != completions + actualInTreatment) {
            warnings.add("Assignments do not equal completions plus patients in treatment.");
        }

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
        report.append(fullRow("\\             both wings compete for the same bays             /"))
                .append('\n');
        report.append(FULL_BORDER).append('\n');
        report.append(fullRow("SHARED TREATMENT BAYS")).append('\n');
        report.append(fullRow("Bays: " + renderBays(bays.totalCapacity(), reportedAvailable))).append('\n');
        report.append(fullRow("Total: " + bays.totalCapacity()
                + " | Reported available: " + reportedAvailable
                + " | Patient states imply available: " + expectedAvailable)).append('\n');
        report.append(FULL_BORDER).append('\n');
        report.append(fullRow("FLOW TOTALS")).append('\n');
        report.append(fullRow("Assignments: " + assignments
                + " | Completions: " + completions
                + " | Currently in treatment: " + actualInTreatment)).append('\n');
        report.append(FULL_BORDER).append('\n');

        if (warnings.isEmpty()) {
            report.append(fullRow("STATUS: HEALTHY - capacity and patient flow reconcile")).append('\n');
        } else {
            report.append(fullRow("STATUS: INCONSISTENT - investigate shared-state concurrency")).append('\n');
            for (String warning : warnings) {
                report.append(fullRow("! " + warning)).append('\n');
            }
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
