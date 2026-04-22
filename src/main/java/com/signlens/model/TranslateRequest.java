package com.signlens.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Request model for translation endpoint
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranslateRequest {
    private String text;

    @JsonProperty("target_language")
    private String targetLanguage;
}
