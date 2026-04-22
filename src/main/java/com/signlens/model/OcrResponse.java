package com.signlens.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Response model returned after OCR processing
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OcrResponse {

    @JsonProperty("detected_text")
    private String detectedText;

    private double confidence;

    @JsonProperty("translated_text")
    private String translatedText;

    @JsonProperty("target_language")
    private String targetLanguage;

    private String status;
    private String message;
}
