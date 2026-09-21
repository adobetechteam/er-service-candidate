package com.skiclinic.er.service;

import com.skiclinic.er.model.Patient;
import com.skiclinic.er.model.PatientStatus;
import com.skiclinic.er.model.Severity;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ERService {
    private final Clock clock;
    private final PatientPrioritizer prioritizer;
    private final List<Patient> patients = new CopyOnWriteArrayList<>();
    private final TreatmentCapacityPool capacityPool;

    public ERService(TreatmentCapacityPool capacityPool, Clock clock) {
        this(capacityPool, clock, new PatientPrioritizer());
    }

    public ERService(TreatmentCapacityPool capacityPool, Clock clock, PatientPrioritizer prioritizer) {
        this.capacityPool = Objects.requireNonNull(capacityPool, "capacityPool");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.prioritizer = Objects.requireNonNull(prioritizer, "prioritizer");
    }

    public Patient admit(String id, String name, Severity severity) {
        Patient patient = new Patient(id, name, severity, clock.instant());
        if (patients.stream().anyMatch(existing -> existing.id().equals(id))) {
            throw new IllegalArgumentException("patient id already exists: " + id);
        }
        patients.add(patient);
        return patient;
    }

    public Optional<TreatmentAssignment> assignNextPatient() {
        if (capacityPool.availableSlots() == 0) {
            return Optional.empty();
        }

        return prioritizer.selectNext(patients, clock.instant())
                .filter(Patient::startTreatment)
                .map(patient -> {
                    capacityPool.reserveSlot();
                    return new TreatmentAssignment(patient, clock.instant());
                });
    }

    public boolean completeTreatment(String patientId) {
        Patient patient = findPatient(patientId)
                .orElseThrow(() -> new IllegalArgumentException("unknown patient: " + patientId));
        if (!patient.discharge()) {
            return false;
        }
        capacityPool.releaseSlot();
        return true;
    }

    public Optional<Patient> findPatient(String patientId) {
        return patients.stream()
                .filter(patient -> patient.id().equals(patientId))
                .findFirst();
    }

    public List<Patient> patients() {
        return List.copyOf(patients);
    }

    public long waitingPatientCount() {
        return patients.stream()
                .filter(patient -> patient.status() == PatientStatus.WAITING)
                .count();
    }

    public int treatmentCapacity() {
        return capacityPool.totalCapacity();
    }

    public int availableTreatmentSlots() {
        return capacityPool.availableSlots();
    }
}

