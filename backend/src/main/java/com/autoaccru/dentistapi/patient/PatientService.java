package com.autoaccru.dentistapi.patient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    // Strip everything except digits (keeps phone numbers consistent)
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[^0-9]", "");
    }

    // Save a new visit.
    // - First time a number appears → mark as new patient
    // - Otherwise → returning patient
    public Patient saveVisit(Patient incoming) {
        String norm = normalizePhone(incoming.getPhoneNumber());
        boolean exists = patientRepository.existsByNormPhone(norm);

        Patient p = new Patient();
        p.setFirstName(incoming.getFirstName());
        p.setLastName(incoming.getLastName());
        p.setAddress(incoming.getAddress());
        p.setPhoneNumber(norm);
        p.setNewPatient(!exists);

        return patientRepository.save(p);
    }

    // Fetch one latest record per phone (used for displaying the table)
    public java.util.List<Patient> listLatestUnique() {
        return patientRepository.findLatestUnique();
    }

    // Used by the “View” dialog to show total visits and first-time status
    public Map<String, Object> statsByPatientId(Long id) {
        Patient p = patientRepository.findById(id).orElseThrow();
        String norm = normalizePhone(p.getPhoneNumber());
        int visits = patientRepository.countVisits(norm);
        boolean firstTimeNew = (visits == 1);

        return Map.of(
                "visit_count", visits,
                "new_patient_first_time", firstTimeNew
        );
    }
    // === added for controller /patients/unique ===
    public java.util.List<Patient> getUniquePatients() {
        // reuse your existing method; no behavior change
        return listLatestUnique();
    }

    // === added for controller /patients/{id}/stats ===
    public java.util.Map<String, Object> getVisitStats(Long id) {
        // reuse your existing method; no behavior change
        return statsByPatientId(id);
    }

}
