package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.QuizQuestionDTO;
import com.aistudybuddy.exception.InvalidQuizException;
import com.aistudybuddy.exception.QuizValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_API_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    public GeminiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(45000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String getModel() {
        return model;
    }

    /**
     * Summarizes notes into simple, structured language with key takeaways and definitions
     */
    public String summarizeNotes(String title, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Study notes content cannot be empty.");
        }

        String systemInstruction = "You are an expert AI academic tutor. Summarize the provided study notes into simple, "
                + "easy-to-understand language. Identify important concepts, key points, and definitions. "
                + "Use headings and bullet points. Preserve important information without changing its meaning.";

        String userPrompt = "Title: " + (title != null && !title.isBlank() ? title : "Study Notes") + "\n\nContent:\n" + content;
        String result = callGemini(systemInstruction, userPrompt);
        if (result == null || result.trim().isEmpty()) {
            throw new IllegalStateException("AI tutor returned an empty summary response. Please try again.");
        }
        return result;
    }

    /**
     * Calls Gemini to generate raw JSON for multiple-choice questions without parsing.
     */
    public String generateQuizRaw(String topic, int numQuestions, String difficulty) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Quiz topic cannot be empty.");
        }
        if (numQuestions <= 0 || numQuestions > 20) {
            throw new IllegalArgumentException("Number of questions must be between 1 and 20.");
        }

        String systemInstruction = "You are an academic examiner. Return ONLY a single raw JSON object with a single field 'questions' "
                + "which is an array of objects. Each question object must have exactly four properties:\n"
                + "1. 'question': string (clear question text)\n"
                + "2. 'options': array of EXACTLY 4 distinct, non-empty strings\n"
                + "3. 'correctAnswer': string that EXACTLY matches one of the 4 options\n"
                + "4. 'explanation': concise student-friendly explanation of why this answer is correct.\n"
                + "Do NOT wrap in markdown formatting. Return strictly valid JSON.";

        String userPrompt = String.format("Generate %d %s multiple-choice questions on the topic: '%s'.",
                numQuestions, difficulty != null ? difficulty : "Medium", topic);

        return callGemini(systemInstruction, userPrompt);
    }

    /**
     * Generates multiple-choice quiz questions using Gemini structured JSON output
     * Strictly validates question count, 4 non-empty options, and correct answer match.
     */
    public List<QuizQuestionDTO> generateQuiz(String topic, int numQuestions, String difficulty) {
        String jsonResponse = generateQuizRaw(topic, numQuestions, difficulty);
        return parseAndValidateQuizQuestions(jsonResponse, numQuestions);
    }

    /**
     * Answers academic doubts clearly with step-by-step guidance
     */
    public String solveDoubt(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be empty.");
        }

        String systemInstruction = "You are an empathetic, encouraging academic tutor. Provide clear, accurate, "
                + "student-friendly explanations with step-by-step breakdowns when required. Use simple language and relatable examples.";

        String result = callGemini(systemInstruction, question);
        if (result == null || result.trim().isEmpty()) {
            throw new IllegalStateException("AI tutor returned an empty doubt solution. Please try again.");
        }
        return result;
    }

    private String callGemini(String systemInstruction, String prompt) {
        String resolvedKey = resolveApiKey();
        if (resolvedKey == null || resolvedKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable.");
        }

        String url = String.format(GEMINI_API_ENDPOINT, model);

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> systemPart = new HashMap<>();
        systemPart.put("parts", List.of(Map.of("text", systemInstruction)));
        requestBody.put("systemInstruction", systemPart);

        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("parts", List.of(Map.of("text", prompt)));
        requestBody.put("contents", List.of(contentPart));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", resolvedKey);
        headers.set("User-Agent", "AI-Study-Buddy/1.0.0");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            throw new IllegalStateException("Gemini API returned an empty candidate response.");
        } catch (HttpStatusCodeException ex) {
            log.error("Gemini API HTTP Error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new IllegalStateException("Gemini API rate limit reached. Please wait a few seconds and try again.");
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new IllegalStateException("Invalid Gemini API key or unauthorized access. Please verify GEMINI_API_KEY.");
            }
            throw new IllegalStateException("Gemini API request failed with status: " + ex.getStatusCode());
        } catch (Exception e) {
            log.error("Gemini service error: {}", e.getMessage());
            throw new RuntimeException("Failed to communicate with AI tutor: " + e.getMessage(), e);
        }
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("MY_GEMINI_API_KEY") && !apiKey.contains("your_gemini")) {
            return apiKey;
        }
        return System.getenv("GEMINI_API_KEY");
    }

    /**
     * Parses and strictly validates quiz questions returned by Gemini AI.
     * Enforces:
     * - Exactly expected question count
     * - Exactly 4 non-empty, distinct options per question
     * - Correct answer matching one of the 4 options
     * - Non-empty explanation
     */
    public List<QuizQuestionDTO> parseAndValidateQuizQuestions(String rawText, int expectedCount) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new QuizValidationException("Gemini returned empty response for quiz generation.");
        }

        String cleaned = rawText.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf("\n");
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Malformed JSON received from Gemini: {}", cleaned);
            throw new QuizValidationException("Gemini returned malformed JSON: " + e.getMessage(), e);
        }

        JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
        if (!questionsNode.isArray()) {
            throw new QuizValidationException("Gemini response missing 'questions' JSON array.");
        }

        List<QuizQuestionDTO> list = new ArrayList<>();
        int index = 0;
        for (JsonNode qNode : questionsNode) {
            index++;
            String question = qNode.path("question").asText().trim();
            if (question.isEmpty()) {
                throw new QuizValidationException("Question #" + index + " text cannot be blank.");
            }

            JsonNode optionsNode = qNode.path("options");
            if (!optionsNode.isArray()) {
                throw new QuizValidationException("Question #" + index + " missing 'options' array.");
            }

            List<String> options = new ArrayList<>();
            Set<String> uniqueOptions = new HashSet<>();
            for (JsonNode opt : optionsNode) {
                String optText = opt.asText().trim();
                if (optText.isEmpty()) {
                    throw new QuizValidationException("Question #" + index + " contains an empty option.");
                }
                options.add(optText);
                uniqueOptions.add(optText.toLowerCase());
            }

            // STRICT REQUIREMENT: Exactly 4 distinct non-empty options
            if (options.size() != 4) {
                throw new QuizValidationException(
                        String.format("Question #%d has %d options. Expected exactly 4 options.", index, options.size())
                );
            }
            if (uniqueOptions.size() != 4) {
                throw new QuizValidationException("Question #" + index + " contains duplicate options.");
            }

            String correctAnswer = qNode.path("correctAnswer").asText().trim();
            if (correctAnswer.isEmpty()) {
                throw new QuizValidationException("Question #" + index + " has an empty correctAnswer.");
            }

            // STRICT REQUIREMENT: correctAnswer must match one of the 4 options
            String matchedOption = null;
            for (String opt : options) {
                if (opt.equalsIgnoreCase(correctAnswer)) {
                    matchedOption = opt;
                    break;
                }
            }
            if (matchedOption == null) {
                throw new QuizValidationException(
                        String.format("Question #%d correctAnswer '%s' does not match any of the 4 options: %s",
                                index, correctAnswer, options)
                );
            }

            String explanation = qNode.path("explanation").asText().trim();
            if (explanation.isEmpty()) {
                explanation = "The correct answer is: " + matchedOption;
            }

            list.add(new QuizQuestionDTO(question, options, matchedOption, explanation));
        }

        // STRICT REQUIREMENT: Validate generated question count
        if (list.size() != expectedCount) {
            throw new QuizValidationException(
                    String.format("AI generated %d questions, expected exactly %d questions.", list.size(), expectedCount)
            );
        }

        return list;
    }
}
