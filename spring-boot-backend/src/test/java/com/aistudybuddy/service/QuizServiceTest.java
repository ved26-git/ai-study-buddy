package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.*;
import com.aistudybuddy.entity.Quiz;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.exception.InvalidQuizException;
import com.aistudybuddy.exception.QuizAlreadySubmittedException;
import com.aistudybuddy.exception.QuizNotFoundException;
import com.aistudybuddy.exception.QuizValidationException;
import com.aistudybuddy.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private QuizService quizService;

    private User sampleUser;
    private Quiz sampleQuiz;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Alex Johnson", "alex@aistudybuddy.com", "secret123");
        sampleUser.setUserId(1L);

        String storedJson = "[" +
                "{\"question\":\"What is Java?\",\"options\":[\"OS\",\"Language\",\"Browser\",\"Protocol\"],\"correctAnswer\":\"Language\",\"explanation\":\"Java is a language.\"}," +
                "{\"question\":\"What is JVM?\",\"options\":[\"Virtual Machine\",\"Browser\",\"Editor\",\"Compiler\"],\"correctAnswer\":\"Virtual Machine\",\"explanation\":\"JVM runs bytecode.\"}," +
                "{\"question\":\"What is bytecode?\",\"options\":[\"Binary code\",\"Source code\",\"Intermediate code\",\"Assembly\"],\"correctAnswer\":\"Intermediate code\",\"explanation\":\"Bytecode is intermediate.\"}," +
                "{\"question\":\"What is JIT?\",\"options\":[\"Just In Time compiler\",\"Java Thread\",\"JSON Parser\",\"JDBC Tool\"],\"correctAnswer\":\"Just In Time compiler\",\"explanation\":\"JIT compiles hot code.\"}," +
                "{\"question\":\"What is OOP?\",\"options\":[\"Object Oriented Programming\",\"Open OS Protocol\",\"Online Operation\",\"Object Output Process\"],\"correctAnswer\":\"Object Oriented Programming\",\"explanation\":\"OOP paradigm.\"}" +
                "]";

        sampleQuiz = new Quiz(sampleUser, "Java OOP", "Easy", storedJson, 5);
        sampleQuiz.setQuizId(50L);
    }

    // ==========================================
    // 1. GENERATE QUIZ TESTS
    // ==========================================

    @Test
    @DisplayName("Should generate quiz with answers hidden from the public client response (QuizAttemptResponse)")
    void testGenerateQuizHidesAnswers() {
        QuizGenerateRequest request = new QuizGenerateRequest("Java OOP", 5, "Easy");

        List<QuizQuestionDTO> questionsFromGemini = List.of(
                new QuizQuestionDTO("What is Java?", List.of("OS", "Language", "Browser", "Protocol"), "Language", "Java is a language."),
                new QuizQuestionDTO("What is JVM?", List.of("Virtual Machine", "Browser", "Editor", "Compiler"), "Virtual Machine", "JVM runs bytecode."),
                new QuizQuestionDTO("What is bytecode?", List.of("Binary code", "Source code", "Intermediate code", "Assembly"), "Intermediate code", "Bytecode is intermediate."),
                new QuizQuestionDTO("What is JIT?", List.of("Just In Time compiler", "Java Thread", "JSON Parser", "JDBC Tool"), "Just In Time compiler", "JIT compiles hot code."),
                new QuizQuestionDTO("What is OOP?", List.of("Object Oriented Programming", "Open OS Protocol", "Online Operation", "Object Output Process"), "Object Oriented Programming", "OOP paradigm.")
        );

        when(geminiService.generateQuiz("Java OOP", 5, "Easy")).thenReturn(questionsFromGemini);
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        QuizAttemptResponse response = quizService.generateQuiz(sampleUser, request);

        assertNotNull(response);
        assertEquals(50L, response.getQuizId());
        assertEquals(5, response.getQuestions().size());
        assertEquals("What is Java?", response.getQuestions().get(0).getQuestion());
        assertEquals(4, response.getQuestions().get(0).getOptions().size());
    }

    @Test
    @DisplayName("RESTRICT QUESTION COUNT: Should reject question count not equal to 5, 10, or 15")
    void testRejectInvalidQuestionCount() {
        QuizGenerateRequest request3 = new QuizGenerateRequest("Java", 3, "Medium");
        InvalidQuizException ex3 = assertThrows(InvalidQuizException.class,
                () -> quizService.generateQuiz(sampleUser, request3));
        assertTrue(ex3.getMessage().contains("Allowed question counts are 5, 10, or 15"));

        QuizGenerateRequest request7 = new QuizGenerateRequest("Java", 7, "Medium");
        InvalidQuizException ex7 = assertThrows(InvalidQuizException.class,
                () -> quizService.generateQuiz(sampleUser, request7));
        assertTrue(ex7.getMessage().contains("Allowed question counts are 5, 10, or 15"));

        QuizGenerateRequest request20 = new QuizGenerateRequest("Java", 20, "Medium");
        InvalidQuizException ex20 = assertThrows(InvalidQuizException.class,
                () -> quizService.generateQuiz(sampleUser, request20));
        assertTrue(ex20.getMessage().contains("Allowed question counts are 5, 10, or 15"));
    }

    @Test
    @DisplayName("RESTRICT DIFFICULTY: Should reject invalid difficulty values")
    void testRejectInvalidDifficulty() {
        QuizGenerateRequest request = new QuizGenerateRequest("Java", 5, "Impossible");
        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.generateQuiz(sampleUser, request));
        assertTrue(ex.getMessage().contains("Allowed difficulty values are Easy, Medium, or Hard"));
    }

    @Test
    @DisplayName("PREVENT DUPLICATE QUESTIONS: Should reject generated quiz with duplicate question text")
    void testValidateQuizQuestionsRejectsDuplicateQuestionText() {
        List<QuizQuestionDTO> duplicateQuestions = List.of(
                new QuizQuestionDTO("What is inheritance?", List.of("A", "B", "C", "D"), "A", "Expl"),
                new QuizQuestionDTO("  what is INHERITANCE?  ", List.of("E", "F", "G", "H"), "E", "Expl")
        );

        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.validateQuizQuestions(duplicateQuestions, 2));

        assertTrue(ex.getMessage().contains("Duplicate question text detected in quiz"));
    }

    // ==========================================
    // 2. SUBMIT QUIZ VALIDATION TESTS
    // ==========================================

    @Test
    @DisplayName("SUCCESSFUL COMPLETE SUBMISSION: Should evaluate all 5 questions and calculate accurate score")
    void testSubmitQuizSuccessfulCompleteSubmission() {
        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(
                0, "Language",
                1, "Virtual Machine",
                2, "Intermediate code",
                3, "Just In Time compiler",
                4, "Object Oriented Programming"
        ));

        when(quizRepository.findByQuizIdAndUserWithLock(50L, sampleUser)).thenReturn(Optional.of(sampleQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        QuizSubmitResponse response = quizService.submitQuiz(50L, sampleUser, submitReq);

        assertNotNull(response);
        assertEquals(5, response.getScore());
        assertEquals(5, response.getTotalQuestions());
        assertEquals(100, response.getPercentage());
    }

    @Test
    @DisplayName("PREVENT INCOMPLETE SUBMISSION: Should reject when fewer answers than total questions are submitted")
    void testSubmitQuizRejectsIncompleteSubmission() {
        // Quiz has 5 questions, but student submitted only 3
        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(
                0, "Language",
                1, "Virtual Machine",
                2, "Intermediate code"
        ));

        when(quizRepository.findByQuizIdAndUserWithLock(50L, sampleUser)).thenReturn(Optional.of(sampleQuiz));

        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.submitQuiz(50L, sampleUser, submitReq));

        assertTrue(ex.getMessage().contains("Incomplete quiz submission"));
        verify(quizRepository, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("MISSING ANSWER: Should reject submission when a specific question index is missing")
    void testSubmitQuizRejectsMissingAnswerIndex() {
        // 5 answers provided, but indexes are 0, 1, 2, 3, 99 (missing index 4)
        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(
                0, "Language",
                1, "Virtual Machine",
                2, "Intermediate code",
                3, "Just In Time compiler",
                99, "Object Oriented Programming"
        ));

        when(quizRepository.findByQuizIdAndUserWithLock(50L, sampleUser)).thenReturn(Optional.of(sampleQuiz));

        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.submitQuiz(50L, sampleUser, submitReq));

        assertTrue(ex.getMessage().contains("Missing answer for question index 4") || ex.getMessage().contains("Invalid question index 99"));
        verify(quizRepository, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("BLANK ANSWER: Should reject submission when an answer is blank")
    void testSubmitQuizRejectsBlankAnswer() {
        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(
                0, "Language",
                1, "Virtual Machine",
                2, "   ", // Blank answer
                3, "Just In Time compiler",
                4, "Object Oriented Programming"
        ));

        when(quizRepository.findByQuizIdAndUserWithLock(50L, sampleUser)).thenReturn(Optional.of(sampleQuiz));

        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.submitQuiz(50L, sampleUser, submitReq));

        assertTrue(ex.getMessage().contains("cannot be null or blank"));
        verify(quizRepository, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("INVALID OPTION TEXT: Should reject submitted answer that is not one of the 4 valid options")
    void testSubmitQuizRejectsInvalidOptionText() {
        // Question 0 options are ["OS", "Language", "Browser", "Protocol"]
        // Student submits "Banana", which is arbitrary text
        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(
                0, "Banana",
                1, "Virtual Machine",
                2, "Intermediate code",
                3, "Just In Time compiler",
                4, "Object Oriented Programming"
        ));

        when(quizRepository.findByQuizIdAndUserWithLock(50L, sampleUser)).thenReturn(Optional.of(sampleQuiz));

        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.submitQuiz(50L, sampleUser, submitReq));

        assertTrue(ex.getMessage().contains("is not one of the valid options for this question"));
        verify(quizRepository, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("CASE-INSENSITIVE OPTION: Should accept submitted answer safely regardless of case")
    void testSubmitQuizAcceptsCaseInsensitiveAnswer() {
        // Option is "Language", student submits lowercase "language"
        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(
                0, "language",
                1, "virtual machine",
                2, "INTERMEDIATE CODE",
                3, "Just In Time compiler",
                4, "object oriented programming"
        ));

        when(quizRepository.findByQuizIdAndUserWithLock(50L, sampleUser)).thenReturn(Optional.of(sampleQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        QuizSubmitResponse response = quizService.submitQuiz(50L, sampleUser, submitReq);

        assertNotNull(response);
        assertEquals(5, response.getScore());
        assertEquals(100, response.getPercentage());
    }

    @Test
    @DisplayName("CONCURRENT / DUPLICATE SUBMISSION: Should throw QuizAlreadySubmittedException (HTTP 409)")
    void testPreventConcurrentDuplicateSubmissionThrows409() {
        sampleQuiz.setCompletedAt(LocalDateTime.now().minusMinutes(2));
        sampleQuiz.setScore(4);

        when(quizRepository.findByQuizIdAndUserWithLock(50L, sampleUser)).thenReturn(Optional.of(sampleQuiz));

        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(
                0, "Language",
                1, "Virtual Machine",
                2, "Intermediate code",
                3, "Just In Time compiler",
                4, "Object Oriented Programming"
        ));

        QuizAlreadySubmittedException ex = assertThrows(QuizAlreadySubmittedException.class,
                () -> quizService.submitQuiz(50L, sampleUser, submitReq));

        assertTrue(ex.getMessage().contains("Duplicate submissions are prohibited"));
        verify(quizRepository, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("NOT FOUND: Should throw QuizNotFoundException (HTTP 404) when quiz ID does not exist")
    void testSubmitQuizNotFoundThrows404() {
        when(quizRepository.findByQuizIdAndUserWithLock(999L, sampleUser)).thenReturn(Optional.empty());

        QuizSubmitRequest submitReq = new QuizSubmitRequest(Map.of(0, "A"));

        assertThrows(QuizNotFoundException.class,
                () -> quizService.submitQuiz(999L, sampleUser, submitReq));
    }

    // ==========================================
    // 3. PARSING & OPTIONS COUNT VALIDATION TESTS
    // ==========================================

    @Test
    @DisplayName("VALIDATION: Should reject question when it has fewer than four options (e.g. 3 options)")
    void testValidateQuizQuestionsRejectsThreeOptions() {
        List<QuizQuestionDTO> threeOptionQuestions = List.of(
                new QuizQuestionDTO("What is Java?", List.of("Language", "OS", "Browser"), "Language", "Expl")
        );

        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.validateQuizQuestions(threeOptionQuestions, 1));

        assertTrue(ex.getMessage().contains("has 3 options. Exactly four options are required per question."));
    }

    @Test
    @DisplayName("VALIDATION: Should reject question when correctAnswer does not match any of the four options")
    void testValidateQuizQuestionsRejectsMismatchedAnswer() {
        List<QuizQuestionDTO> mismatchedQuestions = List.of(
                new QuizQuestionDTO("Which is JVM language?", List.of("Kotlin", "Scala", "Groovy", "Clojure"), "Python", "Expl")
        );

        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.validateQuizQuestions(mismatchedQuestions, 1));

        assertTrue(ex.getMessage().contains("does not exist within the four options"));
    }

    @Test
    @DisplayName("PARSING: Should parse valid Gemini response with 4 options and existing correctAnswer")
    void testParseGeminiQuizResponseSuccess() {
        String json = "{\"questions\":[{\"question\":\"What is an interface?\",\"options\":[\"Contract\",\"Class\",\"Primitive\",\"Package\"],\"correctAnswer\":\"Contract\",\"explanation\":\"Interfaces define contracts.\"}]}";

        List<QuizQuestionDTO> questions = quizService.parseGeminiQuizResponse(json);

        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals("What is an interface?", questions.get(0).getQuestion());
        assertEquals(4, questions.get(0).getOptions().size());
        assertEquals("Contract", questions.get(0).getCorrectAnswer());
    }

    @Test
    @DisplayName("PARSING: Should throw InvalidQuizException when Gemini returns malformed JSON")
    void testParseGeminiQuizResponseThrowsInvalidQuizExceptionWhenMalformedJson() {
        String brokenJson = "{\"questions\": [invalid json...";

        InvalidQuizException ex = assertThrows(InvalidQuizException.class,
                () -> quizService.parseGeminiQuizResponse(brokenJson));

        assertTrue(ex.getMessage().contains("Failed to parse Gemini JSON response"));
    }
}
