package com.skiclinic.er;

import com.skiclinic.er.model.Severity;
import com.skiclinic.er.service.ERService;
import com.skiclinic.er.service.TreatmentAssignment;
import com.skiclinic.er.service.TreatmentCapacityPool;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Main {
    private static final int TOTAL_BAYS = 5;
    private static final int WORKER_THREADS = 16;
    private static final int PATIENTS_PER_WING = 8000;
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

        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS);
        for (int i = 0; i < WORKER_THREADS; i++) {
            ERService wing = (i % 2 == 0) ? mainWing : afterHoursWing;
            pool.submit(() -> runWorker(wing, assignments, completions));
        }

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.MINUTES);

        report(sharedBays, mainWing, afterHoursWing, assignments.get(), completions.get());
    }

    private static void seedPatients(ERService wing, String prefix, Clock clock) {
        Severity[] severities = Severity.values();
        for (int i = 0; i < PATIENTS_PER_WING; i++) {
            wing.admit(prefix + "-" + i, prefix + " Patient " + i, severities[i % severities.length]);
        }
    }

    private static void runWorker(ERService wing, AtomicInteger assignments, AtomicInteger completions) {
        for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
            Optional<TreatmentAssignment> assignment = wing.assignNextPatient();
            if (assignment.isPresent()) {
                assignments.incrementAndGet();
                if (wing.completeTreatment(assignment.get().patient().id())) {
                    completions.incrementAndGet();
                }
            }
        }
    }

    private static void report(TreatmentCapacityPool bays, ERService mainWing, ERService afterHoursWing,
                                int assignments, int completions) {
        System.out.println("=== Ski Town ER — shared bay pool load simulation ===");
        System.out.println("Total bays:            " + bays.totalCapacity());
        System.out.println("Worker threads:        " + WORKER_THREADS);
        System.out.println("Assignments completed: " + assignments);
        System.out.println("Treatments completed:  " + completions);
        System.out.println("Main wing waiting:     " + mainWing.waitingPatientCount());
        System.out.println("After-hours waiting:   " + afterHoursWing.waitingPatientCount());
        System.out.println("Available bays now:    " + bays.availableSlots());
    }

    private Main() {
    }
}
