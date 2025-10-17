package com.autoaccru.dentistapi.patient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientService patientService;

    @GetMapping
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @PostMapping("/voice")
    public Patient addPatientByVoice(@RequestBody Patient patient) {
        return patientRepository.save(patient);
    }

    @PostMapping
    public Patient addPatient(@RequestBody Patient patient) {
        return patientRepository.save(patient);
    }

    // === NEW: deduplicated list for the table (latest record per normalized phone) ===
    @GetMapping("/unique")
    public List<Patient> getAllPatientsUnique() {
        return patientService.getUniquePatients();
    }

    // === NEW: visit stats for the "View" dialog (e.g., total visits) ===
    @GetMapping("/{id}/stats")
    public Map<String, Object> getStats(@PathVariable Long id) {
        return patientService.getVisitStats(id);
    }
}
