package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.*;
import com.aistudybuddy.entity.Quiz;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.exception.InvalidQuizException;
import com.aistudybuddy.exception.QuizAlreadySubmittedException;
import com.aistudybuddy.exception.QuizNotFoundException;
import com.aistudybuddy.exception.QuizValidationException;
import com.aistudybuddy.repository.QuizRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(QuizRepository quizRepository, GeminiService geminiService) {
        this.quizRepository = quizRepository;
        this.geminiService = geminiService;
    }

    /**
     * Generates an AI-powered quiz with strict server-side validation.
     * Restricts question count to 5, 10, or 15 and difficulty to Easy, Medium, or Hard.
     * Parses the Gemini JSON response, verifies that every question contains exactly 4 options,
     * ensures correctAnswer exists within those options, and rejects duplicate question text.
     * CRITICAL SECURITY: Answer keys and explanations are stored strictly in the database.
     * The response to the frontend contains only questions and options via QuizAttemptResponse.
     */
    public QuizAttemptResponse generateQuiz(User user, QuizGenerateRequest request) {
        if (request == null) {
            throw new InvalidQuizException("Quiz generation request cannot be null.");
        }
        if (request.getTopic() == null || request.getTopic().trim().isEmpty()) {
            throw new InvalidQuizException("Quiz topic cannot be empty.");
        }

        // REQUIREMENT 3: RESTRICT QUESTION COUNT TO 5, 10, or 15
        Integer count = request.getNumQuestions();
        if (count == null || (count != 5 && count != 10 && count != 15)) {
            throw new InvalidQuizException(String.format(
                    "Invalid question count: %s. Allowed question counts are 5, 10, or 15.",
                    count
            ));
        }

        // REQUIREMENT 4: RESTRICT DIFFICULTY TO Easy, Medium, Hard
        String rawDiff = request.getDifficulty();
        if (rawDiff == null || rawDiff.trim().isEmpty() ||
                (!rawDiff.trim().equalsIgnoreCase("Easy") &&
                 !rawDiff.trim().equalsIgnoreCase("Medium") &&
                 !rawDiff.trim().equalsIgnoreCase("Hard"))) {
            throw new InvalidQuizException(String.format(
                    "Invalid difficulty: '%s'. Allowed difficulty values are Easy, Medium, or Hard.",
                    rawDiff
            ));
        }
        String difficulty = QuizGenerateRequest.normalizeDifficulty(rawDiff);

        List<QuizQuestionDTO> fullQuestions = geminiService.generateQuiz(request.getTopic().trim(), count, difficulty);
        validateQuizQuestions(fullQuestions, count);

        try {
            String questionsJson = objectMapper.writeValueAsString(fullQuestions);
            Quiz quiz = new Quiz(user, request.getTopic().trim(), difficulty, questionsJson, fullQuestions.size());
            Quiz saved = quizRepository.save(quiz);

            // Sanitize questions: strip correctAnswer and explanation before sending to client
            List<QuizQuestionPublicDTO> publicQuestions = new ArrayList<>();
            for (int i = 0; i < fullQuestions.size(); i++) {
                QuizQuestionDTO q = fullQuestions.get(i);
                publicQuestions.add(new QuizQuestionPublicDTO(i, q.getQuestion(), q.getOptions()));
            }

            return new QuizAttemptResponse(
                    saved.getQuizId(),
                    saved.getTopic(),
                    saved.getDifficulty(),
                    saved.getTotalQuestions(),
                    saved.getCreatedAt().toString(),
                    publicQuestions
            );
        } catch (Exception e) {
            throw new RuntimeException("Error processing quiz questions: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the Gemini JSON response, verifies that every question in the quiz object contains
     * an array of exactly 4 options, ensures that the 'correctAnswer' value exists within those options,
     * and rejects duplicate question text; if any validation fails, throws an InvalidQuizException.
     */
    public List<QuizQuestionDTO> parseGeminiQuizResponse(String jsonResponse) {
        return parseGeminiQuizResponse(jsonResponse, -1);
    }

    /**
     * Parses the Gemini JSON response with expected question count verification.
     */
    public List<QuizQuestionDTO> parseGeminiQuizResponse(String jsonResponse, int expectedCount) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new InvalidQuizException("Gemini returned an empty quiz response.");
        }

        String cleaned = jsonResponse.trim();
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
            throw new InvalidQuizException("Failed to parse Gemini JSON response: " + e.getMessage(), e);
        }

        JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
        if (!questionsNode.isArray()) {
            throw new InvalidQuizException("Invalid Gemini JSON: quiz object must contain a 'questions' array.");
        }

        List<QuizQuestionDTO> questions = new ArrayList<>();
        Set<String> seenQuestionTexts = new HashSet<>();
        int index = 0;

        for (JsonNode qNode : questionsNode) {
            index++;
            String question = qNode.path("question").asText().trim();
            if (question.isEmpty()) {
                throw new InvalidQuizException("Question #" + index + ": Question text cannot be blank.");
            }

            // REQUIREMENT 5: PREVENT DUPLICATE QUESTIONS
            String normalizedQuestion = question.replaceAll("\\s+", " ").toLowerCase();
            if (!seenQuestionTexts.add(normalizedQuestion)) {
                throw new InvalidQuizException(String.format(
                        "Duplicate question text detected in quiz: '%s'. Every question must be distinct.",
                        question
                ));
            }

            JsonNode optionsNode = qNode.path("options");
            if (!optionsNode.isArray()) {
                throw new InvalidQuizException("Question #" + index + ": Missing 'options' array. Every question must contain an array of options.");
            }

            // Verify that every question contains an array of exactly 4 options
            if (optionsNode.size() != 4) {
                throw new InvalidQuizException(String.format(
                        "Question #%d has %d options. Every question in the quiz object must contain an array of exactly 4 options.",
                        index, optionsNode.size()
                ));
            }

            List<String> options = new ArrayList<>();
            Set<String> uniqueOptions = new HashSet<>();
            for (int optIdx = 0; optIdx < optionsNode.size(); optIdx++) {
                String opt = optionsNode.get(optIdx).asText().trim();
                if (opt.isEmpty()) {
                    throw new InvalidQuizException(String.format(
                            "Question #%d option at index %d is empty. Options must be non-empty strings.", index, optIdx
                    ));
                }
                if (!uniqueOptions.add(opt.toLowerCase())) {
                    throw new InvalidQuizException(String.format(
                            "Question #%d contains duplicate option: '%s'. All 4 options must be distinct.", index, opt
                    ));
                }
                options.add(opt);
            }

            String correctAnswer = qNode.path("correctAnswer").asText().trim();
            if (correctAnswer.isEmpty()) {
                throw new InvalidQuizException("Question #" + index + ": 'correctAnswer' field cannot be blank.");
            }

            // Ensure that the correctAnswer value exists within those 4 options
            String matchedOption = null;
            for (String opt : options) {
                if (opt.equalsIgnoreCase(correctAnswer)) {
                    matchedOption = opt;
                    break;
                }
            }

            if (matchedOption == null) {
                throw new InvalidQuizException(String.format(
                        "Question #%d 'correctAnswer' ('%s') does not exist within the 4 options: %s",
                        index, correctAnswer, options
                ));
            }

            String explanation = qNode.path("explanation").asText().trim();
            if (explanation.isEmpty()) {
                explanation = "The correct answer is: " + matchedOption;
            }

            questions.add(new QuizQuestionDTO(question, options, matchedOption, explanation));
        }

        if (expectedCount > 0 && questions.size() != expectedCount) {
            throw new InvalidQuizException(String.format(
                    "AI generated %d questions, expected exactly %d questions.", questions.size(), expectedCount
            ));
        }

        return questions;
    }

    /**
     * Validates an in-memory list of QuizQuestionDTOs against the same strict rules.
     */
    public void validateQuizQuestions(List<QuizQuestionDTO> questions, int expectedCount) {
        if (questions == null || questions.isEmpty()) {
            throw new InvalidQuizException("Gemini response did not contain any quiz questions.");
        }

        if (expectedCount > 0 && questions.size() != expectedCount) {
            throw new InvalidQuizException(String.format(
                    "AI generated %d questions, expected exactly %d questions.", questions.size(), expectedCount
            ));
        }

        Set<String> seenQuestionTexts = new HashSet<>();

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestionDTO q = questions.get(i);
            int qNum = i + 1;

            if (q == null) {
                throw new InvalidQuizException("Question #" + qNum + " is null.");
            }

            if (q.getQuestion() == null || q.getQuestion().trim().isEmpty()) {
                throw new InvalidQuizException("Question #" + qNum + " text cannot be blank.");
            }

            // REQUIREMENT 5: PREVENT DUPLICATE QUESTIONS
            String normalizedQuestion = q.getQuestion().trim().replaceAll("\\s+", " ").toLowerCase();
            if (!seenQuestionTexts.add(normalizedQuestion)) {
                throw new InvalidQuizException(String.format(
                        "Duplicate question text detected in quiz: '%s'. Every question must be distinct.",
                        q.getQuestion().trim()
                ));
            }

            List<String> options = q.getOptions();
            if (options == null) {
                throw new InvalidQuizException("Question #" + qNum + " options list cannot be null.");
            }

            // STRICT REQUIREMENT: Exactly four options per question
            if (options.size() != 4) {
                throw new InvalidQuizException(String.format(
                        "Question #%d has %d options. Exactly four options are required per question.",
                        qNum, options.size()
                ));
            }

            Set<String> uniqueOptions = new HashSet<>();
            for (int optIdx = 0; optIdx < options.size(); optIdx++) {
                String opt = options.get(optIdx);
                if (opt == null || opt.trim().isEmpty()) {
                    throw new InvalidQuizException(String.format(
                            "Question #%d contains an empty or blank option at index %d.", qNum, optIdx
                    ));
                }
                if (!uniqueOptions.add(opt.trim().toLowerCase())) {
                    throw new InvalidQuizException(String.format(
                            "Question #%d contains duplicate option: '%s'.", qNum, opt.trim()
                    ));
                }
            }

            String correctAnswer = q.getCorrectAnswer();
            if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
                throw new InvalidQuizException("Question #" + qNum + " 'correctAnswer' cannot be blank.");
            }

            // STRICT REQUIREMENT: 'correctAnswer' value exists within those four options
            String matchedOption = null;
            for (String opt : options) {
                if (opt.trim().equalsIgnoreCase(correctAnswer.trim())) {
                    matchedOption = opt.trim();
                    break;
                }
            }

            if (matchedOption == null) {
                throw new InvalidQuizException(String.format(
                        "Question #%d 'correctAnswer' '%s' does not exist within the four options: %s",
                        qNum, correctAnswer, options
                ));
            }

            // Normalize correctAnswer to match the exact option string
            q.setCorrectAnswer(matchedOption);

            if (q.getExplanation() == null || q.getExplanation().trim().isEmpty()) {
                q.setExplanation("The correct answer is: " + matchedOption);
            }
        }
    }

    /**
     * Alias for parseGeminiQuizResponse for backward compatibility.
     */
    public List<QuizQuestionDTO> parseAndValidateQuizJson(String rawJson, int expectedCount) {
        return parseGeminiQuizResponse(rawJson, expectedCount);
    }

    /**
     * Server-side grading: evaluates student's submitted answers against the stored answer key.
     * Transactional with locking to prevent concurrent double submission.
     * Strictly verifies that every question is answered and that submitted choices match the question's options.
     */
    @Transactional
    public QuizSubmitResponse submitQuiz(Long quizId, User user, QuizSubmitRequest request) {
        if (quizId == null) {
            throw new InvalidQuizException("Quiz ID cannot be null.");
        }

        // Retrieve the quiz using a pessimistic write lock.
        // Do not fall back to an unlocked query if locking fails.
        Optional<Quiz> quizOpt =
        quizRepository.findByQuizIdAndUserWithLock(quizId, user);

        Quiz quiz = quizOpt.orElseThrow(() -> new QuizNotFoundException("Quiz not found with ID: " + quizId));

        // REQUIREMENT 6 & 7: PREVENT CONCURRENT DOUBLE SUBMISSION & RETURN 409 CONFLICT
        if (quiz.getCompletedAt() != null || quiz.getScore() != null) {
            throw new QuizAlreadySubmittedException(String.format(
                    "Quiz ID %d has already been submitted and graded on %s. Duplicate submissions are prohibited.",
                    quizId, quiz.getCompletedAt()
            ));
        }

        // REQUIREMENT 1: PREVENT INCOMPLETE QUIZ SUBMISSION
        if (request == null || request.getAnswers() == null) {
            throw new InvalidQuizException("Submitted answers cannot be null.");
        }

        Map<Integer, String> submitted = request.getAnswers();
        int totalQuestions = quiz.getTotalQuestions();

        if (submitted.size() != totalQuestions) {
            throw new InvalidQuizException(String.format(
                    "Incomplete quiz submission. Every question must be answered: received %d answers for %d questions.",
                    submitted.size(), totalQuestions
            ));
        }

        // Retrieve stored questions
        List<QuizQuestionDTO> questions;
        try {
            questions = objectMapper.readValue(
                    quiz.getQuestions(),
                    new TypeReference<List<QuizQuestionDTO>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Error reading stored quiz questions: " + e.getMessage(), e);
        }

        // Validate each question has an answer and it belongs to that question's four options
        Map<Integer, String> normalizedSubmitted = new HashMap<>();

        for (int i = 0; i < totalQuestions; i++) {
            if (!submitted.containsKey(i)) {
                throw new InvalidQuizException(String.format(
                        "Missing answer for question index %d. Every question must be answered.", i
                ));
            }

            String answer = submitted.get(i);
            if (answer == null || answer.trim().isEmpty()) {
                throw new InvalidQuizException(String.format(
                        "Answer for question index %d cannot be null or blank.", i
                ));
            }

            // REQUIREMENT 2: VALIDATE SUBMITTED OPTIONS
            QuizQuestionDTO questionDto = questions.get(i);
            List<String> validOptions = questionDto.getOptions();

            String matchedOption = null;
            for (String opt : validOptions) {
                if (opt.trim().equalsIgnoreCase(answer.trim())) {
                    matchedOption = opt.trim();
                    break;
                }
            }

            if (matchedOption == null) {
                throw new InvalidQuizException(String.format(
                        "Submitted answer for question #%d ('%s') is not one of the valid options for this question: %s",
                        i + 1, answer.trim(), validOptions
                ));
            }

            normalizedSubmitted.put(i, matchedOption);
        }

        // Also check if any out-of-bounds keys were included
        for (Integer key : submitted.keySet()) {
            if (key == null || key < 0 || key >= totalQuestions) {
                throw new InvalidQuizException(String.format(
                        "Invalid question index %d. Valid question indexes for this quiz are 0 through %d.",
                        key, totalQuestions - 1
                ));
            }
        }

        // Score calculation safely on the server
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            String chosen = normalizedSubmitted.get(i);
            if (chosen != null && chosen.equalsIgnoreCase(questions.get(i).getCorrectAnswer().trim())) {
                score++;
            }
        }

        try {
            quiz.setScore(score);
            quiz.setSubmittedAnswers(objectMapper.writeValueAsString(normalizedSubmitted));
            quiz.setCompletedAt(LocalDateTime.now());
            quizRepository.save(quiz);

            int percentage = Math.round(((float) score / totalQuestions) * 100);

            return new QuizSubmitResponse(quiz.getQuizId(), score, totalQuestions, percentage, questions, normalizedSubmitted);
        } catch (Exception e) {
            throw new RuntimeException("Error saving quiz submission: " + e.getMessage(), e);
        }
    }

    /**
     * Safe quiz list retrieval: returns QuizListResponse DTOs.
     * Raw Quiz entities with questions JSON and answer keys are NEVER exposed.
     */
    public List<QuizListResponse> getUserQuizzes(User user) {
        List<Quiz> quizzes = quizRepository.findByUserOrderByCreatedAtDesc(user);
        return quizzes.stream().map(this::mapToQuizListResponse).collect(Collectors.toList());
    }

    /**
     * Safe retrieval of a specific quiz:
     * If quiz is already submitted: returns QuizSubmitResponse (review view).
     * If quiz is in progress / not submitted: returns QuizAttemptResponse (answers and explanations redacted).
     */
    public Object getQuizDetailsSafe(Long quizId, User user) {
        Quiz quiz = quizRepository.findByQuizIdAndUser(quizId, user)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with ID: " + quizId));

        try {
            List<QuizQuestionDTO> fullQuestions = objectMapper.readValue(
                    quiz.getQuestions(),
                    new TypeReference<List<QuizQuestionDTO>>() {}
            );

            if (quiz.getCompletedAt() != null) {
                // Completed quiz: safe to reveal explanations & correct answers
                Map<Integer, String> submitted = Collections.emptyMap();
                if (quiz.getSubmittedAnswers() != null && !quiz.getSubmittedAnswers().isBlank()) {
                    submitted = objectMapper.readValue(quiz.getSubmittedAnswers(), new TypeReference<Map<Integer, String>>() {});
                }
                int percentage = quiz.getTotalQuestions() > 0 && quiz.getScore() != null
                        ? Math.round(((float) quiz.getScore() / quiz.getTotalQuestions()) * 100)
                        : 0;

                return new QuizSubmitResponse(quiz.getQuizId(), quiz.getScore() != null ? quiz.getScore() : 0,
                        quiz.getTotalQuestions(), percentage, fullQuestions, submitted);
            } else {
                // In-progress quiz: sanitize questions, conceal correct answers and explanations
                List<QuizQuestionPublicDTO> publicQuestions = new ArrayList<>();
                for (int i = 0; i < fullQuestions.size(); i++) {
                    QuizQuestionDTO q = fullQuestions.get(i);
                    publicQuestions.add(new QuizQuestionPublicDTO(i, q.getQuestion(), q.getOptions()));
                }
                return new QuizAttemptResponse(
                        quiz.getQuizId(),
                        quiz.getTopic(),
                        quiz.getDifficulty(),
                        quiz.getTotalQuestions(),
                        quiz.getCreatedAt().toString(),
                        publicQuestions
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading quiz data: " + e.getMessage(), e);
        }
    }

    public void deleteQuiz(Long quizId, User user) {
        Quiz quiz = quizRepository.findByQuizIdAndUser(quizId, user)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with ID: " + quizId));
        quizRepository.delete(quiz);
    }

    public QuizListResponse mapToQuizListResponse(Quiz quiz) {
        return new QuizListResponse(
                quiz.getQuizId(),
                quiz.getTopic(),
                quiz.getDifficulty(),
                quiz.getTotalQuestions(),
                quiz.getScore(),
                quiz.getCreatedAt() != null ? quiz.getCreatedAt().toString() : null,
                quiz.getCompletedAt() != null ? quiz.getCompletedAt().toString() : null
        );
    }
}
