package com.cscd488seniorproject.syllabussyncproject.service;

// Jackson types for building and reading JSON trees without POJO classes
import com.fasterxml.jackson.databind.JsonNode;       // generic read-only JSON node (used for return type)
import com.fasterxml.jackson.databind.ObjectMapper;   // main Jackson utility: serializes/deserializes JSON
import com.fasterxml.jackson.databind.node.ArrayNode; // mutable JSON array node (e.g. the "messages" list)
import com.fasterxml.jackson.databind.node.ObjectNode;// mutable JSON object node (e.g. {"key": "value"})

import org.springframework.beans.factory.annotation.Value; // reads values from application.properties
import org.springframework.stereotype.Service;             // marks this class as a Spring-managed service bean

// Java built-in HTTP client (introduced in Java 11)
import java.net.URI;                        // represents the target URL
import java.net.http.HttpClient;            // sends HTTP requests
import java.net.http.HttpRequest;           // represents a single HTTP request
import java.net.http.HttpResponse;          // holds the response status code and body

// Spring registers this class as a singleton service bean available for dependency injection
@Service
public class ClaudeService {

    // The Anthropic Messages API endpoint that accepts our JSON payload
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    // The Claude model to use for parsing — Sonnet 4 balances speed and accuracy
    private static final String MODEL = "claude-sonnet-4-20250514";

    // Required header telling Anthropic which version of their API spec we target
    private static final String API_VERSION = "2023-06-01";

    // Upper bound on how many tokens Claude may produce in its response
    // 1500 is enough for the JSON schema without wasting quota
    private static final int MAX_TOKENS = 1500;

    // The system prompt instructs Claude to act as a structured syllabus parser.
    // It defines the exact JSON shape to return, the confidence rubric, and edge-case rules.
    // Using a text block (""") keeps the multi-line string readable without concatenation.
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

    // Injected at startup from the "anthropic.api.key" property (e.g. in application.properties or env var)
    @Value("${anthropic.api.key}")
    private String apiKey;

    // Reusable Jackson mapper — shared instance is thread-safe for read/write operations
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Reusable HTTP client — creating one per request is wasteful; this is shared across calls
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Sends the base64-encoded PDF to Claude and returns the parsed syllabus as a JsonNode.
     * The returned node matches the exact schema defined in the system prompt.
     */
    public JsonNode parseSyllabus(String pdfBase64) throws Exception {
        // Start building the top-level JSON body for the API request
        ObjectNode requestBody = objectMapper.createObjectNode();

        // Tell Claude which model to use
        requestBody.put("model", MODEL);

        // Cap the response length so we don't exceed quota or wait unnecessarily
        requestBody.put("max_tokens", MAX_TOKENS);

        // Attach the system prompt — this sets Claude's role and output schema before the user turn
        requestBody.put("system", SYSTEM_PROMPT);

        // The Messages API expects a "messages" array; create the array and the single user message
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();

        // Mark this message as coming from the user (as opposed to "assistant")
        userMessage.put("role", "user");

        // Claude supports multi-part content blocks; we send two: the PDF document + a text instruction
        ArrayNode content = objectMapper.createArrayNode();

        // Document block — Claude reads the PDF directly from base64
        ObjectNode docBlock = objectMapper.createObjectNode();
        docBlock.put("type", "document"); // tells Claude this block is a document (PDF) rather than text

        // "source" describes where/how the document data is provided
        ObjectNode source = objectMapper.createObjectNode();
        source.put("type", "base64");               // data is base64-encoded, not a URL
        source.put("media_type", "application/pdf"); // MIME type so Claude knows it's a PDF
        source.put("data", pdfBase64);               // the actual base64 PDF bytes

        docBlock.set("source", source);  // nest the source object inside the document block
        content.add(docBlock);           // add the document block to the content array

        // Text prompt
        ObjectNode textBlock = objectMapper.createObjectNode();
        textBlock.put("type", "text"); // second block is plain text
        textBlock.put("text", "Extract structured information from the syllabus PDF above."); // the instruction
        content.add(textBlock); // add the text block after the document block

        // Attach the content array to the user message, then add the message to the messages array
        userMessage.set("content", content);
        messages.add(userMessage);

        // Attach the messages array to the top-level request body
        requestBody.set("messages", messages);

        // Build the HTTP POST request with all required Anthropic headers
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))                          // target the messages endpoint
                .header("Content-Type", "application/json")        // body is JSON
                .header("x-api-key", apiKey)                       // authenticate with our API key
                .header("anthropic-version", API_VERSION)          // specify the API version contract
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody))) // serialize the body
                .build();

        // Send the request synchronously and collect the full response body as a String
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Any non-200 status means Claude rejected or failed the request — surface the details
        if (response.statusCode() != 200) {
            throw new RuntimeException("Claude API error " + response.statusCode() + ": " + response.body());
        }

        // Parse the raw JSON response string into a navigable tree
        JsonNode responseNode = objectMapper.readTree(response.body());

        // The text we care about is nested at: content[0].text
        // "content" is an array of blocks; we always expect one text block at index 0
        String rawText = responseNode.path("content").get(0).path("text").asText();

        // Strip accidental markdown fences before parsing
        // Claude sometimes wraps output in ```json ... ``` despite the system prompt telling it not to
        rawText = rawText.strip();
        if (rawText.startsWith("```")) {
            // Remove the opening fence (with optional "json" tag) and the closing fence
            rawText = rawText.replaceFirst("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").strip();
        }

        // Parse the cleaned JSON string into a JsonNode and return it to the caller
        return objectMapper.readTree(rawText);
    }
}
