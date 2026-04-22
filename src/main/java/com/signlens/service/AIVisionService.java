package com.signlens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.signlens.model.OcrResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * AIVisionService
 * Handles image OCR and translation via the Claude AI API.
 * Uses Java's built-in HttpClient (Java 11+).
 */
@Service
public class AIVisionService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-sonnet-4-20250514";

    // Map of language codes to display names
    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<>();
    static {
        LANGUAGE_NAMES.put("en", "English");
        LANGUAGE_NAMES.put("kn", "Kannada");
        LANGUAGE_NAMES.put("te", "Telugu");
        LANGUAGE_NAMES.put("ta", "Tamil");
        LANGUAGE_NAMES.put("mr", "Marathi");
        LANGUAGE_NAMES.put("ml", "Malayalam");
        LANGUAGE_NAMES.put("hi", "Hindi");
        LANGUAGE_NAMES.put("fr", "French");
        LANGUAGE_NAMES.put("es", "Spanish");
        LANGUAGE_NAMES.put("ar", "Arabic");
    }

    @Value("${anthropic.api.key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newHttpClient();

    /**
     * Process an image file: extract text via OCR and translate it.
     *
     * @param file           the uploaded image
     * @param targetLanguage BCP-47 language code (e.g. "kn", "te")
     * @return OcrResponse with detected and translated text
     */
    public OcrResponse processImage(MultipartFile file, String targetLanguage) throws IOException, InterruptedException {
        // Convert image to base64
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

        String langName = LANGUAGE_NAMES.getOrDefault(targetLanguage, "English");

        // Build system prompt
        String systemPrompt = buildSystemPrompt(langName);

        // Build request body as JSON
        String requestBody = buildRequestBody(base64Image, mimeType, langName, systemPrompt);

        // Determine API key: env var takes priority, then config
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null || key.isEmpty()) key = apiKey;

        // Call Claude API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", key != null ? key : "")
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error " + response.statusCode() + ": " + response.body());
        }

        // Parse response
        return parseApiResponse(response.body(), targetLanguage);
    }

    /**
     * Build the system prompt for OCR + translation.
     */
    private String buildSystemPrompt(String langName) {
        return "You are a precise street sign OCR and translation engine.\n" +
               "Analyze the image and:\n" +
               "1. Extract ALL visible text from street sign(s) exactly as written.\n" +
               "2. Translate it accurately into " + langName + ".\n" +
               "3. Estimate OCR confidence as a percentage 0-100.\n\n" +
               "Return ONLY this exact JSON (no markdown, no extra text):\n" +
               "{\"detected\":\"<exact sign text>\",\"translated\":\"<" + langName + " translation>\",\"confidence\":<0-100>}\n\n" +
               "Rules:\n" +
               "- If multiple signs, join text with ' | '\n" +
               "- If no text visible, set detected='' and confidence=0\n" +
               "- Never add explanations outside the JSON";
    }

    /**
     * Build the JSON request body for the Claude API.
     */
    private String buildRequestBody(String base64Image, String mimeType, String langName, String systemPrompt) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", MODEL);
        root.put("max_tokens", 1000);
        root.put("system", systemPrompt);

        // Messages array
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");

        // Content array (image + text)
        ArrayNode content = message.putArray("content");

        // Image block
        ObjectNode imageBlock = content.addObject();
        imageBlock.put("type", "image");
        ObjectNode source = imageBlock.putObject("source");
        source.put("type", "base64");
        source.put("media_type", mimeType);
        source.put("data", base64Image);

        // Text block
        ObjectNode textBlock = content.addObject();
        textBlock.put("type", "text");
        textBlock.put("text", "Extract text from this street sign and translate to " + langName + ". Return JSON only.");

        return objectMapper.writeValueAsString(root);
    }

    /**
     * Parse the Claude API response into an OcrResponse.
     */
    private OcrResponse parseApiResponse(String responseBody, String targetLanguage) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentArray = root.path("content");

        String rawText = "";
        for (JsonNode block : contentArray) {
            if ("text".equals(block.path("type").asText())) {
                rawText = block.path("text").asText();
                break;
            }
        }

        // Clean JSON string
        rawText = rawText.replaceAll("```json", "").replaceAll("```", "").trim();

        // Parse inner JSON
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(rawText);
        } catch (Exception e) {
            // Try to extract JSON object from response
            int start = rawText.indexOf('{');
            int end   = rawText.lastIndexOf('}');
            if (start >= 0 && end > start) {
                parsed = objectMapper.readTree(rawText.substring(start, end + 1));
            } else {
                throw new RuntimeException("Could not parse AI response.");
            }
        }

        String detected   = parsed.path("detected").asText("").trim();
        String translated = parsed.path("translated").asText("").trim();
        double confidence = parsed.path("confidence").asDouble(80.0);

        if (detected.isEmpty()) {
            throw new RuntimeException("No text detected in the image. Try a clearer photo.");
        }

        return OcrResponse.builder()
                .detectedText(detected)
                .translatedText(translated)
                .confidence(Math.min(100, Math.max(0, confidence)))
                .targetLanguage(targetLanguage)
                .status("success")
                .message("Text detected and translated successfully.")
                .build();
    }
}
