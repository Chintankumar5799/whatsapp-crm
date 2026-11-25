package com.appointment.notification.controller;

import com.appointment.notification.model.MessageTemplate;
import com.appointment.notification.service.TemplateService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<MessageTemplate>> getTemplates(@RequestParam(name = "clinicId", defaultValue = "1") Long clinicId) {
        return ResponseEntity.ok(templateService.getClinicTemplates(clinicId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageTemplate> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request.getTitle(), request.getTemplateBody(), request.getIsActive()));
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, String>> previewTemplate(@RequestBody PreviewRequest request) {
        String rendered = templateService.previewTemplate(request.getTemplateBody(), request.getCustomPlaceholders());
        return ResponseEntity.ok(Map.of("renderedText", rendered));
    }

    @Data
    public static class UpdateTemplateRequest {
        private String title;
        private String templateBody;
        private Boolean isActive;
    }

    @Data
    public static class PreviewRequest {
        private String templateBody;
        private Map<String, String> customPlaceholders;
    }
}

