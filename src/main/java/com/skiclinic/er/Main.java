package com.skiclinic.er;

import com.skiclinic.er.model.Severity;
import com.skiclinic.er.service.ERService;
import com.skiclinic.er.service.TreatmentAssignment;
import com.skiclinic.er.service.TreatmentCapacityPool;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Main {
    private static final int TOTAL_BAYS = 5;
    private static final int WORKER_THREADS = 16;
    private static final int PATIENTS_PER_WING = 8000;
    private static final int TOTAL_PATIENTS = PATIENTS_PER_WING * 2;
    private static final int ITERATIONS_PER_THREAD = 4000;

    public static void main(String[] args) throws InterruptedException {
        Clock clock = Clock.systemUTC();
        TreatmentCapacityPool sharedBays = new TreatmentCapacityPool(TOTAL_BAYS);
        ERService mainWing = new ERService(sharedBays, clock);
        ERService afterHoursWing = new ERService(sharedBays, clock);

        seedPatients(mainWing, "MAIN", clock);
        seedPatients(afterHoursWing, "AH", clock);

        AtomicInteger assignments = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        Set<String> dischargedPatientIds = ConcurrentHashMap.newKeySet();
        AtomicBoolean allPatientsDischarged = new AtomicBoolean();

        long treatmentStartedAt = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS);
        for (int i = 0; i < WORKER_THREADS; i++) {
            ERService wing = (i % 2 == 0) ? mainWing : afterHoursWing;
            pool.submit(() -> runWorker(
                    wing,
                    assignments,
                    completions,
                    dischargedPatientIds,
                    allPatientsDischarged));
        }

        pool.shutdown();
        boolean workersFinished = pool.awaitTermination(2, TimeUnit.MINUTES);
        if (!workersFinished) {
            pool.shutdownNow();
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - treatmentStartedAt);

        System.out.print(ERReport.render(
                sharedBays,
                mainWing,
                afterHoursWing,
                assignments.get(),
                completions.get(),
                elapsed,
                workersFinished));
    }

    private static void seedPatients(ERService wing, String prefix, Clock clock) {
        Severity[] severities = Severity.values();
        for (int i = 0; i < PATIENTS_PER_WING; i++) {
            wing.admit(prefix + "-" + i, prefix + " Patient " + i, severities[i % severities.length]);
        }
    }

    private static void runWorker(ERService wing,
                                  AtomicInteger assignments,
                                  AtomicInteger completions,
                                  Set<String> dischargedPatientIds,
                                  AtomicBoolean allPatientsDischarged) {
        for (int i = 0; i < ITERATIONS_PER_THREAD && !allPatientsDischarged.get(); i++) {
            Optional<TreatmentAssignment> assignment = wing.assignNextPatient();
            if (assignment.isPresent()) {
                assignments.incrementAndGet();
                if (wing.completeTreatment(assignment.get().patient().id())) {
                    completions.incrementAndGet();
                    if (dischargedPatientIds.add(assignment.get().patient().id())
                            && dischargedPatientIds.size() == TOTAL_PATIENTS) {
                        allPatientsDischarged.set(true);
                    }
                }
            }
        }
    }

    private Main() {
    }
}
