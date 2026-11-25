package com.appointment.clinic.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "clinics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clinic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "India";

    @Column(name = "prescription_header_html", columnDefinition = "TEXT")
    private String prescriptionHeaderHtml;

    @Column(name = "prescription_footer_html", columnDefinition = "TEXT")
    private String prescriptionFooterHtml;

    @Column(name = "google_review_url", columnDefinition = "TEXT")
    private String googleReviewUrl;

    @Column(name = "upi_id", length = 100)
    @Builder.Default
    private String upiId = "pms.clinic@upi";

    @Column(name = "follow_up_validity_days", nullable = false)
    @Builder.Default
    private Integer followUpValidityDays = 30;

    @Column(name = "new_case_consultation_fee")
    @Builder.Default
    private BigDecimal newCaseConsultationFee = BigDecimal.valueOf(500.00);

    @Column(name = "old_case_consultation_fee")
    @Builder.Default
    private BigDecimal oldCaseConsultationFee = BigDecimal.valueOf(300.00);

    @Column(name = "subscription_plan", nullable = false, length = 30)
    @Builder.Default
    private String subscriptionPlan = "TRIAL";

    @Column(name = "subscription_status", nullable = false, length = 30)
    @Builder.Default
    private String subscriptionStatus = "ACTIVE";

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "subscription_ends_at")
    private LocalDateTime subscriptionEndsAt;

    @Column(name = "max_doctors", nullable = false)
    @Builder.Default
    private Integer maxDoctors = 3;

    @Column(name = "max_staff", nullable = false)
    @Builder.Default
    private Integer maxStaff = 10;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

