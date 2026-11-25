package com.appointment.notification.service;

import com.appointment.clinic.model.Clinic;
import com.appointment.clinic.repository.ClinicRepository;
import com.appointment.notification.model.MessageTemplate;
import com.appointment.notification.repository.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final MessageTemplateRepository templateRepository;
    private final ClinicRepository clinicRepository;

    public List<MessageTemplate> getClinicTemplates(Long clinicId) {
        Long cid = clinicId != null ? clinicId : 1L;
        List<MessageTemplate> templates = templateRepository.findByClinicIdOrderByIdAsc(cid);
        if (templates.isEmpty() && !cid.equals(1L)) {
            // Fallback to clinic 1 default templates if custom ones don't exist yet
            return templateRepository.findByClinicIdOrderByIdAsc(1L);
        }
        return templates;
    }

    public MessageTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message template not found: " + id));
    }

    @Transactional
    public MessageTemplate updateTemplate(Long id, String title, String body, Boolean isActive) {
        MessageTemplate template = getTemplateById(id);
        if (title != null && !title.trim().isEmpty()) {
            template.setTitle(title.trim());
        }
        if (body != null && !body.trim().isEmpty()) {
            template.setTemplateBody(body.trim());
        }
        if (isActive != null) {
            template.setIsActive(isActive);
        }
        log.info("Updated message template id={}: title='{}'", id, template.getTitle());
        return templateRepository.save(template);
    }

    /**
     * Resolves template text with dynamic runtime placeholder values
     */
    public String renderMessage(Long clinicId, String templateType, Map<String, String> values) {
        Long cid = clinicId != null ? clinicId : 1L;
        Optional<MessageTemplate> tplOpt = templateRepository.findByClinicIdAndTemplateType(cid, templateType);
        
        if (tplOpt.isEmpty() && !cid.equals(1L)) {
            tplOpt = templateRepository.findByClinicIdAndTemplateType(1L, templateType);
        }

        if (tplOpt.isEmpty() || !Boolean.TRUE.equals(tplOpt.get().getIsActive())) {
            return null; // Signals caller to use hardcoded default
        }

        String content = tplOpt.get().getTemplateBody();

        // Default clinic metadata
        Clinic clinic = clinicRepository.findById(cid).orElse(null);
        if (clinic != null) {
            if (!values.containsKey("clinic_name")) {
                values.put("clinic_name", clinic.getName());
            }
            if (!values.containsKey("clinic_phone")) {
                values.put("clinic_phone", clinic.getPhone() != null ? clinic.getPhone() : "");
            }
            if (!values.containsKey("review_link")) {
                values.put("review_link", clinic.getGoogleReviewUrl() != null ? clinic.getGoogleReviewUrl() : "");
            }
        }

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String val = entry.getValue() != null ? entry.getValue() : "";
            content = content.replace(placeholder, val);
        }

        return content;
    }

    public String previewTemplate(String templateBody, Map<String, String> sampleValues) {
        if (templateBody == null) return "";
        Map<String, String> mock = new HashMap<>();
        mock.put("patient_name", "Rahul Sharma");
        mock.put("doctor_name", "Dr. Amit Verma");
        mock.put("date", "2026-11-25");
        mock.put("time", "10:30 AM");
        mock.put("clinic_name", "PMS Health Care");
        mock.put("clinic_phone", "+91 98765 43210");
        mock.put("booking_id", "BK-1082");
        mock.put("request_notes", "General health checkup and BP consultation");
        mock.put("prescription_link", "https://pms.app/rx/demo-rx-101");
        mock.put("review_link", "https://g.page/r/pms-clinic/review");

        if (sampleValues != null) {
            mock.putAll(sampleValues);
        }

        String result = templateBody;
        for (Map.Entry<String, String> entry : mock.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}

