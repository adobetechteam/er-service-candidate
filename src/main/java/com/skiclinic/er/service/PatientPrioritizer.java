package com.skiclinic.er.service;

import com.skiclinic.er.model.Patient;
import com.skiclinic.er.model.PatientStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class PatientPrioritizer {
    public Optional<Patient> selectNext(Collection<Patient> patients, Instant now) {
        Objects.requireNonNull(patients, "patients");
        Objects.requireNonNull(now, "now");

        return patients.stream()
                .filter(patient -> patient.status() == PatientStatus.WAITING)
                .min(Comparator.comparing(Patient::arrivalTime)
                        .thenComparing(Patient::id));
    }
}

