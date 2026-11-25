package com.appointment.clinic.service;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.model.Booking;
import com.appointment.booking.model.Patient;
import com.appointment.booking.repository.BookingRepository;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.booking.service.PatientService;
import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.clinical.model.Consultation;
import com.appointment.clinical.repository.ConsultationRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExchangeService {

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final PatientService patientService;
    private final BookingRepository bookingRepository;
    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Data
    @Builder
    public static class ImportResult {
        private int totalRows;
        private int importedCount;
        private int skippedCount;
        private List<String> errorMessages;
    }

    @Transactional
    public ImportResult importPatientsFromCsv(Long clinicId, String csvContent) throws IOException {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found: " + clinicId));

        int total = 0;
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (isHeader) {
                    isHeader = false;
                    // If header contains "name" or "phone", skip it
                    if (line.toLowerCase().contains("name") || line.toLowerCase().contains("phone")) {
                        continue;
                    }
                }

                total++;
                // Split by comma handling basic CSV
                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (tokens.length < 2) {
                    skipped++;
                    errors.add("Row " + total + ": Insufficient columns (must have Name and Phone)");
                    continue;
                }

                String name = cleanToken(tokens[0]);
                String phone = cleanToken(tokens[1]);

                if (name.isEmpty() || phone.isEmpty()) {
                    skipped++;
                    errors.add("Row " + total + ": Missing Name or Phone");
                    continue;
                }

                // Clean phone
                phone = phone.replaceAll("[^0-9+]", "");
                if (phone.length() < 7) {
                    skipped++;
                    errors.add("Row " + total + ": Invalid phone number: " + phone);
                    continue;
                }

                // Check duplicate phone in clinic
                List<Patient> existing = patientRepository.findByClinicIdAndPhone(clinicId, phone);
                if (!existing.isEmpty()) {
                    skipped++;
                    errors.add("Row " + total + ": Phone " + phone + " already registered as " + existing.get(0).getPatientUid());
                    continue;
                }

                Integer age = null;
                if (tokens.length > 2 && !tokens[2].trim().isEmpty()) {
                    try {
                        age = Integer.parseInt(cleanToken(tokens[2]));
                    } catch (NumberFormatException ignored) {}
                }

                String gender = tokens.length > 3 ? cleanToken(tokens[3]) : null;
                String bloodGroup = tokens.length > 4 ? cleanToken(tokens[4]) : null;
                String address = tokens.length > 5 ? cleanToken(tokens[5]) : null;
                String allergies = tokens.length > 6 ? cleanToken(tokens[6]) : null;
                String chronicDiseases = tokens.length > 7 ? cleanToken(tokens[7]) : null;

                String patientUid = patientService.generatePatientUid(clinicId);

                Patient patient = Patient.builder()
                        .clinicId(clinicId)
                        .patientUid(patientUid)
                        .name(name)
                        .phone(phone)
                        .age(age)
                        .gender(gender)
                        .bloodGroup(bloodGroup)
                        .address(address)
                        .allergies(allergies)
                        .chronicDiseases(chronicDiseases)
                        .totalVisitsCount(0)
                        .isMerged(false)
                        .build();

                patientRepository.save(patient);
                imported++;
            }
        }

        log.info("CSV import completed for clinic {}: total={}, imported={}, skipped={}", clinicId, total, imported, skipped);
        return ImportResult.builder()
                .totalRows(total)
                .importedCount(imported)
                .skippedCount(skipped)
                .errorMessages(errors)
                .build();
    }

    private String cleanToken(String token) {
        if (token == null) return "";
        String s = token.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s.trim();
    }

    public String exportPatientsCsv(Long clinicId) {
        List<Patient> patients = patientRepository.findAll().stream()
                .filter(p -> p.getClinicId().equals(clinicId))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("Patient ID,Full Name,Phone,Age,Gender,Blood Group,Address,Allergies,Chronic Diseases,Total Visits,Last Visit Date,Created At\n");

        for (Patient p : patients) {
            sb.append(escapeCsv(p.getPatientUid())).append(",");
            sb.append(escapeCsv(p.getName())).append(",");
            sb.append(escapeCsv(p.getPhone())).append(",");
            sb.append(p.getAge() != null ? p.getAge() : "").append(",");
            sb.append(escapeCsv(p.getGender())).append(",");
            sb.append(escapeCsv(p.getBloodGroup())).append(",");
            sb.append(escapeCsv(p.getAddress())).append(",");
            sb.append(escapeCsv(p.getAllergies())).append(",");
            sb.append(escapeCsv(p.getChronicDiseases())).append(",");
            sb.append(p.getTotalVisitsCount() != null ? p.getTotalVisitsCount() : 0).append(",");
            sb.append(p.getLastVisitDate() != null ? p.getLastVisitDate().format(DATE_FORMATTER) : "").append(",");
            sb.append(p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_TIME_FORMATTER) : "").append("\n");
        }

        return sb.toString();
    }

    public String exportAppointmentsCsv(Long clinicId) {
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> clinicId.equals(b.getClinicId()))
                .collect(Collectors.toList());

        Map<Long, String> doctorNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (k1, k2) -> k1));

        StringBuilder sb = new StringBuilder();
        sb.append("Booking ID,Appointment Date,Start Time,Patient Name,Phone,Doctor Name,Status,Case Type,Source,Amount,Payment Status\n");

        for (Booking b : bookings) {
            sb.append(b.getId()).append(",");
            sb.append(b.getBookingDate() != null ? b.getBookingDate().toString() : "").append(",");
            sb.append(escapeCsv(b.getStartTime() != null ? b.getStartTime().toString() : "")).append(",");
            sb.append(escapeCsv(b.getPatientName())).append(",");
            sb.append(escapeCsv(b.getPatientPhone())).append(",");
            sb.append(escapeCsv(doctorNames.getOrDefault(b.getDoctorId(), "Doctor #" + b.getDoctorId()))).append(",");
            sb.append(escapeCsv(b.getStatus() != null ? b.getStatus().name() : "")).append(",");
            sb.append(escapeCsv(b.getCaseType() != null ? b.getCaseType() : "")).append(",");
            sb.append(escapeCsv(b.getAppointmentSource() != null ? b.getAppointmentSource() : "")).append(",");
            sb.append(b.getTotalAmount() != null ? b.getTotalAmount().toString() : "0.00").append(",");
            sb.append(escapeCsv(b.getPaymentStatus() != null ? b.getPaymentStatus() : "")).append("\n");
        }

        return sb.toString();
    }

    public String exportConsultationsCsv(Long clinicId) {
        List<Consultation> consultations = consultationRepository.findByClinicIdOrderByCreatedAtDesc(clinicId);
        Map<Long, String> doctorNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (k1, k2) -> k1));
        Map<Long, Patient> patientMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(Patient::getId, p -> p, (p1, p2) -> p1));

        StringBuilder sb = new StringBuilder();
        sb.append("Consultation ID,Date,Patient UID,Patient Name,Doctor Name,Diagnosis,Chief Complaint,Fee,Case Classification,Discharge Condition\n");

        for (Consultation c : consultations) {
            Patient p = patientMap.get(c.getPatientId());
            sb.append(c.getId()).append(",");
            sb.append(c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_TIME_FORMATTER) : "").append(",");
            sb.append(escapeCsv(p != null ? p.getPatientUid() : "N/A")).append(",");
            sb.append(escapeCsv(p != null ? p.getName() : "Unknown")).append(",");
            sb.append(escapeCsv(doctorNames.getOrDefault(c.getDoctorId(), "Doctor #" + c.getDoctorId()))).append(",");
            sb.append(escapeCsv(c.getDiagnosis())).append(",");
            sb.append(escapeCsv(c.getChiefComplaint())).append(",");
            sb.append(c.getConsultationFee() != null ? c.getConsultationFee().toString() : "0.00").append(",");
            sb.append(escapeCsv(c.getCaseType() != null ? c.getCaseType().name() : "")).append(",");
            sb.append(escapeCsv(c.getDischargeCondition() != null ? c.getDischargeCondition() : "Active")).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
