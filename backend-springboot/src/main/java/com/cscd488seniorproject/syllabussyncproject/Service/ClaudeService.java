package com.cscd488seniorproject.syllabussyncproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class ClaudeService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-20250514";
    private static final String API_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1500;

    private static final String SYSTEM_PROMPT = """
            You are a syllabus parser. Extract structured information from the provided syllabus PDF.
            Return ONLY a valid JSON object with no extra text, no markdown, no code fences.
            For each field include a confidence value: "high" if clearly and explicitly stated, \
            "medium" if implied or partially stated, "low" if absent or ambiguous.
            Return this exact shape:
            {
              "classMeetingTimes": { "value": { "days": ["MON","WED","FRI"], "startTime": "09:00", "endTime": "09:50", "location": "CAT 220" }, "confidence": "high|medium|low" },
              "officeHours": { "value": "...", "confidence": "high|medium|low" },
              "gradeScale": { "value": [{ "letter": "A", "range": "93-100%" }], "confidence": "high|medium|low" },
              "gradeBreakdown": { "value": [{ "component": "Assignments", "weight": "60%" }], "confidence": "high|medium|low" },
              "passConditions": { "value": ["Minimum 70% exam average required to pass"], "confidence": "high|medium|low" },
              "attendance": { "value": { "tracked": "yes|no|partial", "affectsGrade": "yes|no|extra_credit", "details": "..." }, "confidence": "high|medium|low" },
              "dueDates": { "value": [{ "name": "HW 1", "date": "Jan 14" }], "confidence": "high|medium|low" },
              "lateWorkPolicy": { "value": "...", "confidence": "high|medium|low" },
              "aiPolicy": { "value": "... or null", "confidence": "high|medium|low" }
            }
            Notes: passConditions captures requirements beyond the grade scale. \
            dueDates: infer specific dates from week ranges + stated due-day patterns and set confidence to medium. \
            aiPolicy: null + low confidence if not mentioned.""";

    @Value("${anthropic.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Sends the base64-encoded PDF to Claude and returns the parsed syllabus as a JsonNode.
     * The returned node matches the exact schema defined in the system prompt.
     */
    public JsonNode parseSyllabus(String pdfBase64) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("system", SYSTEM_PROMPT);

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");

        ArrayNode content = objectMapper.createArrayNode();

        // Document block — Claude reads the PDF directly from base64
        ObjectNode docBlock = objectMapper.createObjectNode();
        docBlock.put("type", "document");
        ObjectNode source = objectMapper.createObjectNode();
        source.put("type", "base64");
        source.put("media_type", "application/pdf");
        source.put("data", pdfBase64);
        docBlock.set("source", source);
        content.add(docBlock);

        // Text prompt
        ObjectNode textBlock = objectMapper.createObjectNode();
        textBlock.put("type", "text");
        textBlock.put("text", "Extract structured information from the syllabus PDF above.");
        content.add(textBlock);

        userMessage.set("content", content);
        messages.add(userMessage);
        requestBody.set("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Claude API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode responseNode = objectMapper.readTree(response.body());
        String rawText = responseNode.path("content").get(0).path("text").asText();

        // Strip accidental markdown fences before parsing
        rawText = rawText.strip();
        if (rawText.startsWith("```")) {
            rawText = rawText.replaceFirst("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").strip();
        }

        return objectMapper.readTree(rawText);
    }
}
