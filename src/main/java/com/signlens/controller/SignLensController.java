package com.signlens.controller;

import com.signlens.model.OcrResponse;
import com.signlens.service.AIVisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * SignLensController
 * REST API endpoints for the SignLens application.
 *
 * Endpoints:
 *   GET  /api/health         - Health check
 *   POST /api/process-image  - OCR + translate an uploaded image
 *   GET  /api/languages      - List supported languages
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")  // Allow all origins for development
public class SignLensController {

    @Autowired
    private AIVisionService aiVisionService;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    /**
     * GET /api/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "SignLens API v2.0",
                "java", System.getProperty("java.version")
        ));
    }

    /**
     * GET /api/languages
     * Returns the list of supported translation languages.
     */
    @GetMapping("/languages")
    public ResponseEntity<?> languages() {
        List<Map<String, String>> langs = List.of(
                Map.of("code", "en", "name", "English",   "script", "Latin"),
                Map.of("code", "kn", "name", "Kannada",   "script", "ಕನ್ನಡ"),
                Map.of("code", "te", "name", "Telugu",    "script", "తెలుగు"),
                Map.of("code", "ta", "name", "Tamil",     "script", "தமிழ்"),
                Map.of("code", "mr", "name", "Marathi",   "script", "मराठी"),
                Map.of("code", "ml", "name", "Malayalam", "script", "മലയാളം"),
                Map.of("code", "hi", "name", "Hindi",     "script", "हिन्दी"),
                Map.of("code", "fr", "name", "French",    "script", "Latin"),
                Map.of("code", "es", "name", "Spanish",   "script", "Latin"),
                Map.of("code", "ar", "name", "Arabic",    "script", "عربي")
        );
        return ResponseEntity.ok(Map.of("languages", langs, "count", langs.size()));
    }

    /**
     * POST /api/process-image
     * Upload an image, extract text via OCR, and translate it.
     *
     * @param file           image file (jpg/png/webp)
     * @param targetLanguage BCP-47 language code
     */
    @PostMapping(value = "/process-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "en") String targetLanguage
    ) {
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Uploaded file is empty."
            ));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Unsupported file type. Use JPG, PNG, or WEBP."
            ));
        }

        try {
            OcrResponse result = aiVisionService.processImage(file, targetLanguage);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }
}
