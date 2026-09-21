package com.skiclinic.er.model;

import java.time.Instant;
import java.util.Objects;

public final class Patient {
    private final String id;
    private final String name;
    private final Severity severity;
    private final Instant arrivalTime;
    private PatientStatus status;

    public Patient(String id, String name, Severity severity, Instant arrivalTime) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.arrivalTime = Objects.requireNonNull(arrivalTime, "arrivalTime");
        this.status = PatientStatus.WAITING;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Severity severity() {
        return severity;
    }

    public Instant arrivalTime() {
        return arrivalTime;
    }

    public PatientStatus status() {
        return status;
    }

    public boolean startTreatment() {
        if (status != PatientStatus.WAITING) {
            return false;
        }
        status = PatientStatus.IN_TREATMENT;
        return true;
    }

    public boolean discharge() {
        if (status != PatientStatus.IN_TREATMENT) {
            return false;
        }
        status = PatientStatus.DISCHARGED;
        return true;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}

