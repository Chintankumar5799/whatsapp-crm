package com.appointment.config;

import com.appointment.auth.model.User;
import com.appointment.auth.repository.UserRepository;
import com.appointment.booking.model.Patient;
import com.appointment.booking.repository.PatientRepository;
import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.clinical.model.Medicine;
import com.appointment.clinical.model.OpdQueue;
import com.appointment.clinical.repository.MedicineRepository;
import com.appointment.clinical.repository.OpdQueueRepository;
import com.appointment.crm.model.PatientInquiry;
import com.appointment.crm.repository.PatientInquiryRepository;
import com.appointment.notification.model.MessageTemplate;
import com.appointment.notification.repository.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final OpdQueueRepository opdQueueRepository;
    private final PatientInquiryRepository patientInquiryRepository;
    private final MessageTemplateRepository messageTemplateRepository;
    private final MedicineRepository medicineRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and seeding initial clinic PMS demo data...");

        // 1. Seed Clinic
        Clinic clinic;
        if (clinicRepository.count() == 0) {
            clinic = Clinic.builder()
                    .code("PMS-MAIN")
                    .name("PMS Healthcare Clinic")
                    .phone("+919876543210")
                    .email("contact@pms.com")
                    .whatsappNumber("+919876543210")
                    .addressLine1("102 Medical Arts Building")
                    .city("Bengaluru")
                    .state("Karnataka")
                    .postalCode("560001")
                    .country("India")
                    .upiId("pmsclinic@okhdfcbank")
                    .followUpValidityDays(30)
                    .newCaseConsultationFee(new BigDecimal("500.00"))
                    .oldCaseConsultationFee(new BigDecimal("300.00"))
                    .isActive(true)
                    .build();
            clinic = clinicRepository.save(clinic);
            log.info("Seeded default clinic: {}", clinic.getName());
        } else {
            clinic = clinicRepository.findAll().get(0);
        }

        // 2. Seed Users
        String universalPassword = passwordEncoder.encode("admin123");

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(universalPassword)
                    .phone("+919876543200")
                    .role(User.UserRole.ADMIN)
                    .clinicId(clinic.getId())
                    .name("Clinic Administrator")
                    .consultationFee(new BigDecimal("500.00"))
                    .oldCaseFee(new BigDecimal("300.00"))
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("Seeded admin user: admin / admin123");
        }

        User doctorUser;
        if (userRepository.findByUsername("dr.smith").isEmpty()) {
            doctorUser = User.builder()
                    .username("dr.smith")
                    .email("dr.smith@example.com")
                    .password(universalPassword)
                    .phone("+919876543210")
                    .role(User.UserRole.DOCTOR)
                    .clinicId(clinic.getId())
                    .name("Dr. John Smith")
                    .registrationNumber("MCI-2021-99881")
                    .qualification("MBBS, MD (General Medicine)")
                    .consultationFee(new BigDecimal("500.00"))
                    .oldCaseFee(new BigDecimal("300.00"))
                    .isActive(true)
                    .build();
            doctorUser = userRepository.save(doctorUser);
            log.info("Seeded doctor user: dr.smith / admin123");
        } else {
            doctorUser = userRepository.findByUsername("dr.smith").get();
        }

        if (userRepository.findByUsername("reception").isEmpty()) {
            User reception = User.builder()
                    .username("reception")
                    .email("reception@example.com")
                    .password(universalPassword)
                    .phone("+919876543211")
                    .role(User.UserRole.RECEPTIONIST)
                    .clinicId(clinic.getId())
                    .name("Sarah Jenkins")
                    .isActive(true)
                    .build();
            userRepository.save(reception);
            log.info("Seeded receptionist user: reception / admin123");
        }

        // 3. Seed Patients
        Patient patient1 = null;
        Patient patient3 = null;
        if (patientRepository.count() == 0) {
            patient1 = Patient.builder()
                    .clinicId(clinic.getId())
                    .patientUid("PMS-MAIN-2026-00001")
                    .name("Rahul Sharma")
                    .phone("+919876543210")
                    .email("rahul.sharma@example.com")
                    .age(38)
                    .gender("MALE")
                    .bloodGroup("B+")
                    .address("45 MG Road, Bengaluru")
                    .allergies("Penicillin")
                    .chronicDiseases("Hypertension")
                    .totalVisitsCount(3)
                    .lastVisitDate(LocalDateTime.now().minusDays(10))
                    .build();
            patient1 = patientRepository.save(patient1);

            Patient patient2 = Patient.builder()
                    .clinicId(clinic.getId())
                    .patientUid("PMS-MAIN-2026-00002")
                    .name("Priya Patel")
                    .phone("+919876543211")
                    .email("priya.patel@example.com")
                    .age(29)
                    .gender("FEMALE")
                    .bloodGroup("O+")
                    .address("12 Indiranagar, Bengaluru")
                    .allergies("None")
                    .chronicDiseases("None")
                    .totalVisitsCount(1)
                    .lastVisitDate(LocalDateTime.now().minusDays(40))
                    .build();
            patientRepository.save(patient2);

            patient3 = Patient.builder()
                    .clinicId(clinic.getId())
                    .patientUid("PMS-MAIN-2026-00003")
                    .name("Amit Verma")
                    .phone("+919876543212")
                    .email("amit.verma@example.com")
                    .age(45)
                    .gender("MALE")
                    .bloodGroup("A+")
                    .address("78 Whitefield, Bengaluru")
                    .allergies("Sulfa drugs")
                    .chronicDiseases("Type 2 Diabetes")
                    .totalVisitsCount(2)
                    .lastVisitDate(LocalDateTime.now().minusDays(5))
                    .build();
            patient3 = patientRepository.save(patient3);
            log.info("Seeded 3 sample patients with valid MRNs");
        }

        // 4. Seed OPD Queue Tokens
        if (opdQueueRepository.count() == 0 && patient1 != null && patient3 != null) {
            OpdQueue token1 = OpdQueue.builder()
                    .clinicId(clinic.getId())
                    .doctorId(doctorUser.getId())
                    .patientId(patient1.getId())
                    .queueNumber(1)
                    .queueDate(LocalDate.now())
                    .status(OpdQueue.QueueStatus.WAITING)
                    .arrivalTime(LocalDateTime.now().minusMinutes(20))
                    .build();
            opdQueueRepository.save(token1);

            OpdQueue token2 = OpdQueue.builder()
                    .clinicId(clinic.getId())
                    .doctorId(doctorUser.getId())
                    .patientId(patient3.getId())
                    .queueNumber(2)
                    .queueDate(LocalDate.now())
                    .status(OpdQueue.QueueStatus.WAITING)
                    .arrivalTime(LocalDateTime.now().minusMinutes(10))
                    .build();
            opdQueueRepository.save(token2);
            log.info("Seeded 2 waiting OPD queue tokens for today");
        }

        // 5. Seed CRM Leads
        if (patientInquiryRepository.count() == 0) {
            PatientInquiry inquiry1 = PatientInquiry.builder()
                    .clinicId(clinic.getId())
                    .name("Ananya Sen")
                    .phone("+919876543220")
                    .requestDescription("Inquiring about consultation timings for diabetic neuropathy and second opinion.")
                    .source(PatientInquiry.InquirySource.GOOGLE_BUSINESS)
                    .status(PatientInquiry.InquiryStatus.NEW)
                    .build();
            patientInquiryRepository.save(inquiry1);

            PatientInquiry inquiry2 = PatientInquiry.builder()
                    .clinicId(clinic.getId())
                    .name("Vikas Gupta")
                    .phone("+919876543221")
                    .requestDescription("Wants to schedule an annual preventive health checkup and blood profile.")
                    .source(PatientInquiry.InquirySource.WHATSAPP)
                    .status(PatientInquiry.InquiryStatus.CONTACTED)
                    .build();
            patientInquiryRepository.save(inquiry2);
            log.info("Seeded 2 sample CRM leads");
        }

        // 6. Seed Medicines
        if (medicineRepository.count() == 0) {
            List<Medicine> medicines = List.of(
                    Medicine.builder().clinicId(clinic.getId()).name("Paracetamol").brandName("Dolo 650").dosageForm("TABLET").strength("650mg").build(),
                    Medicine.builder().clinicId(clinic.getId()).name("Amoxicillin").brandName("Novamox").dosageForm("CAPSULE").strength("500mg").build(),
                    Medicine.builder().clinicId(clinic.getId()).name("Metformin").brandName("Glycomet").dosageForm("TABLET").strength("500mg").build(),
                    Medicine.builder().clinicId(clinic.getId()).name("Amlodipine").brandName("Amlong").dosageForm("TABLET").strength("5mg").build(),
                    Medicine.builder().clinicId(clinic.getId()).name("Pantoprazole").brandName("Pan 40").dosageForm("TABLET").strength("40mg").build(),
                    Medicine.builder().clinicId(clinic.getId()).name("Cetirizine").brandName("Cetzine").dosageForm("TABLET").strength("10mg").build(),
                    Medicine.builder().clinicId(clinic.getId()).name("Azithromycin").brandName("Azee 500").dosageForm("TABLET").strength("500mg").build()
            );
            medicineRepository.saveAll(medicines);
            log.info("Seeded 7 common medicines into catalogue");
        }

        // 7. Seed Message Templates
        if (messageTemplateRepository.count() == 0) {
            List<MessageTemplate> templates = List.of(
                    MessageTemplate.builder()
                            .clinicId(clinic.getId())
                            .templateType("BOOKING_CONFIRMATION")
                            .title("Booking Confirmation")
                            .templateBody("Dear {patient_name}, your appointment with Dr. {doctor_name} at {clinic_name} is confirmed for {date} at {time}. Please arrive 10 minutes before your slot.")
                            .isActive(true)
                            .build(),
                    MessageTemplate.builder()
                            .clinicId(clinic.getId())
                            .templateType("REMINDER_24H")
                            .title("24-Hour Reminder")
                            .templateBody("Hello {patient_name}, this is a gentle reminder for your consultation tomorrow ({date} at {time}) with Dr. {doctor_name} at {clinic_name}.")
                            .isActive(true)
                            .build(),
                    MessageTemplate.builder()
                            .clinicId(clinic.getId())
                            .templateType("FOLLOWUP_REVISIT")
                            .title("Follow-up Revisit Recall")
                            .templateBody("Hello {patient_name}, your follow-up visit with Dr. {doctor_name} at {clinic_name} is due. Please reply or contact {clinic_phone} to reserve your preferred slot.")
                            .isActive(true)
                            .build(),
                    MessageTemplate.builder()
                            .clinicId(clinic.getId())
                            .templateType("PRESCRIPTION_SHARE")
                            .title("Digital Prescription Link")
                            .templateBody("Dear {patient_name}, here is your digital prescription link from Dr. {doctor_name}: {prescription_link}. Wishing you a fast recovery!")
                            .isActive(true)
                            .build(),
                    MessageTemplate.builder()
                            .clinicId(clinic.getId())
                            .templateType("REVIEW_REQUEST")
                            .title("Google 5-Star Review Request")
                            .templateBody("Dear {patient_name}, thank you for choosing {clinic_name}. Please rate your experience: {review_link}. Your feedback helps others find trusted care!")
                            .isActive(true)
                            .build()
            );
            messageTemplateRepository.saveAll(templates);
            log.info("Seeded 5 WhatsApp communication templates");
        }

        log.info("PMS demo data initialization complete!");
    }
}

