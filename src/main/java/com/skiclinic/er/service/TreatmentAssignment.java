package com.skiclinic.er.service;

import com.skiclinic.er.model.Patient;

import java.time.Instant;
import java.util.Objects;

public final class TreatmentAssignment {
    private final Patient patient;
    private final Instant assignedAt;

    public TreatmentAssignment(Patient patient, Instant assignedAt) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
    }

    public Patient patient() {
        return patient;
    }

    public Instant assignedAt() {
        return assignedAt;
    }
}
