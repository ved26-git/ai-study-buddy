package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.QuizQuestionDTO;
import com.aistudybuddy.exception.QuizValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiQuizValidationTest {

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService();
    }

    @Test
    @DisplayName("Should successfully parse valid Gemini JSON with 4 options and matching answer")
    void testParseValidQuizQuestions() {
        String validJson = "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"question\": \"What is polymorphism in Java?\",\n" +
                "      \"options\": [\"Ability to take multiple forms\", \"Inheriting fields only\", \"Private variable access\", \"Compiling code to bytecode\"],\n" +
                "      \"correctAnswer\": \"Ability to take multiple forms\",\n" +
                "      \"explanation\": \"Polymorphism allows one interface to be used for a general class of actions.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        List<QuizQuestionDTO> questions = geminiService.parseAndValidateQuizQuestions(validJson, 1);

        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals(4, questions.get(0).getOptions().size());
        assertEquals("Ability to take multiple forms", questions.get(0).getCorrectAnswer());
    }

    @Test
    @DisplayName("Should reject response when question count is less than requested")
    void testRejectMismatchedQuestionCount() {
        String jsonWithOneQuestion = "{\"questions\": [{\"question\":\"Q1\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswer\":\"A\",\"explanation\":\"E\"}]}";

        QuizValidationException ex = assertThrows(QuizValidationException.class,
                () -> geminiService.parseAndValidateQuizQuestions(jsonWithOneQuestion, 5));

        assertTrue(ex.getMessage().contains("expected exactly 5 questions"));
    }

    @Test
    @DisplayName("Should reject questions that do not have exactly 4 options")
    void testRejectNonFourOptions() {
        String jsonWithThreeOptions = "{\"questions\": [{\"question\":\"Q1\",\"options\":[\"Option 1\",\"Option 2\",\"Option 3\"],\"correctAnswer\":\"Option 1\",\"explanation\":\"E\"}]}";

        QuizValidationException ex = assertThrows(QuizValidationException.class,
                () -> geminiService.parseAndValidateQuizQuestions(jsonWithThreeOptions, 1));

        assertTrue(ex.getMessage().contains("Expected exactly 4 options"));
    }

    @Test
    @DisplayName("Should reject questions with duplicate options")
    void testRejectDuplicateOptions() {
        String jsonWithDuplicates = "{\"questions\": [{\"question\":\"Q1\",\"options\":[\"Duplicate\",\"Duplicate\",\"Option 3\",\"Option 4\"],\"correctAnswer\":\"Duplicate\",\"explanation\":\"E\"}]}";

        QuizValidationException ex = assertThrows(QuizValidationException.class,
                () -> geminiService.parseAndValidateQuizQuestions(jsonWithDuplicates, 1));

        assertTrue(ex.getMessage().contains("duplicate options"));
    }

    @Test
    @DisplayName("Should reject questions where correctAnswer does not match any of the 4 options")
    void testRejectCorrectAnswerMismatch() {
        String jsonMismatch = "{\"questions\": [{\"question\":\"Q1\",\"options\":[\"Apple\",\"Banana\",\"Cherry\",\"Date\"],\"correctAnswer\":\"Elderberry\",\"explanation\":\"E\"}]}";

        QuizValidationException ex = assertThrows(QuizValidationException.class,
                () -> geminiService.parseAndValidateQuizQuestions(jsonMismatch, 1));

        assertTrue(ex.getMessage().contains("does not match any of the 4 options"));
    }

    @Test
    @DisplayName("Should normalize case-insensitive match for correctAnswer")
    void testNormalizeCaseInsensitiveAnswer() {
        String jsonCaseDiff = "{\"questions\": [{\"question\":\"Q1\",\"options\":[\"Encapsulation\",\"Polymorphism\",\"Inheritance\",\"Abstraction\"],\"correctAnswer\":\"encapsulation\",\"explanation\":\"E\"}]}";

        List<QuizQuestionDTO> questions = geminiService.parseAndValidateQuizQuestions(jsonCaseDiff, 1);

        assertEquals("Encapsulation", questions.get(0).getCorrectAnswer());
    }

    @Test
    @DisplayName("Should reject malformed JSON returned by AI")
    void testRejectMalformedJson() {
        String brokenJson = "{\"questions\": [broken json content...";

        assertThrows(QuizValidationException.class,
                () -> geminiService.parseAndValidateQuizQuestions(brokenJson, 1));
    }
}
