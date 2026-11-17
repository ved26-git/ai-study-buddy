package com.aistudybuddy.dto;

import com.aistudybuddy.dto.validator.ValidQuestionCount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

public class StudyDTOs {

    // ==========================================
    // 1. NOTES SUMMARIZER DTOS
    // ==========================================
    public static class SummarizeRequest {
        private String title;

        @NotBlank(message = "Original study content cannot be empty")
        private String content;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class NoteResponse {
        private Long noteId;
        private String title;
        private String originalContent;
        private String summary;
        private String createdAt;

        public NoteResponse() {}

        public NoteResponse(Long noteId, String title, String originalContent, String summary, String createdAt) {
            this.noteId = noteId;
            this.title = title;
            this.originalContent = originalContent;
            this.summary = summary;
            this.createdAt = createdAt;
        }

        public Long getNoteId() { return noteId; }
        public void setNoteId(Long noteId) { this.noteId = noteId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getOriginalContent() { return originalContent; }
        public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // ==========================================
    // 2. QUIZ GENERATOR DTOS (SECURE SCORING)
    // ==========================================
    public static class QuizGenerateRequest {
        @NotBlank(message = "Quiz topic cannot be empty")
        private String topic;

        @NotNull(message = "Question count is required")
        @ValidQuestionCount(message = "Question count must be exactly 5, 10, or 15")
        private Integer numQuestions = 5;

        @NotBlank(message = "Difficulty is required")
        @Pattern(regexp = "^(?i)(easy|medium|hard)$", message = "Difficulty must be one of: Easy, Medium, Hard")
        private String difficulty = "Medium";

        public QuizGenerateRequest() {}

        public QuizGenerateRequest(String topic, Integer numQuestions, String difficulty) {
            this.topic = topic;
            this.numQuestions = numQuestions;
            this.difficulty = normalizeDifficulty(difficulty);
        }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public Integer getNumQuestions() { return numQuestions; }
        public void setNumQuestions(Integer numQuestions) { this.numQuestions = numQuestions; }
        public String getDifficulty() { return normalizeDifficulty(difficulty); }
        public void setDifficulty(String difficulty) { this.difficulty = normalizeDifficulty(difficulty); }

        public static String normalizeDifficulty(String diff) {
            if (diff == null || diff.trim().isEmpty()) {
                return null;
            }
            String trimmed = diff.trim();
            if (trimmed.equalsIgnoreCase("easy")) return "Easy";
            if (trimmed.equalsIgnoreCase("medium")) return "Medium";
            if (trimmed.equalsIgnoreCase("hard")) return "Hard";
            return trimmed;
        }
    }

    /**
     * DTO for Quiz Summary in history / list view.
     * Prevents raw questions JSON from being leaked over the wire.
     */
    public static class QuizListResponse {
        private Long quizId;
        private String topic;
        private String difficulty;
        private int totalQuestions;
        private Integer score;
        private String createdAt;
        private String completedAt;

        public QuizListResponse() {}

        public QuizListResponse(Long quizId, String topic, String difficulty, int totalQuestions,
                                Integer score, String createdAt, String completedAt) {
            this.quizId = quizId;
            this.topic = topic;
            this.difficulty = difficulty;
            this.totalQuestions = totalQuestions;
            this.score = score;
            this.createdAt = createdAt;
            this.completedAt = completedAt;
        }

        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    }

    /**
     * Public Question DTO sent to client before quiz submission.
     * ZERO access to correctAnswer or explanation fields.
     */
    public static class QuizQuestionPublicDTO {
        private int index;
        private String question;
        private List<String> options;

        public QuizQuestionPublicDTO() {}

        public QuizQuestionPublicDTO(int index, String question, List<String> options) {
            this.index = index;
            this.question = question;
            this.options = options;
        }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
    }

    /**
     * Quiz Attempt DTO delivered when starting or attempting a quiz.
     * Contains only sanitized questions (options only, NO answer keys or explanations).
     */
    public static class QuizAttemptResponse {
        private Long quizId;
        private String topic;
        private String difficulty;
        private int totalQuestions;
        private String createdAt;
        private List<QuizQuestionPublicDTO> questions;

        public QuizAttemptResponse() {}

        public QuizAttemptResponse(Long quizId, String topic, String difficulty, int totalQuestions,
                                   String createdAt, List<QuizQuestionPublicDTO> questions) {
            this.quizId = quizId;
            this.topic = topic;
            this.difficulty = difficulty;
            this.totalQuestions = totalQuestions;
            this.createdAt = createdAt;
            this.questions = questions;
        }

        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public List<QuizQuestionPublicDTO> getQuestions() { return questions; }
        public void setQuestions(List<QuizQuestionPublicDTO> questions) { this.questions = questions; }
    }

    // Alias for backwards compatibility with existing clients/tests
    public static class QuizStartResponse extends QuizAttemptResponse {
        public QuizStartResponse() { super(); }
        public QuizStartResponse(Long quizId, String topic, String difficulty, int totalQuestions,
                                 String createdAt, List<QuizQuestionPublicDTO> questions) {
            super(quizId, topic, difficulty, totalQuestions, createdAt, questions);
        }
    }

    /**
     * Full internal Question representation stored securely on server with answer key.
     */
    public static class QuizQuestionDTO {
        private String question;
        private List<String> options;
        private String correctAnswer;
        private String explanation;

        public QuizQuestionDTO() {}

        public QuizQuestionDTO(String question, List<String> options, String correctAnswer, String explanation) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
            this.explanation = explanation;
        }

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    public static class QuizSubmitRequest {
        @NotNull(message = "Submitted answers map is required")
        @NotEmpty(message = "Submitted answers map cannot be empty")
        private Map<Integer, String> answers; // question index -> option string chosen

        public QuizSubmitRequest() {}

        public QuizSubmitRequest(Map<Integer, String> answers) {
            this.answers = answers;
        }

        public Map<Integer, String> getAnswers() { return answers; }
        public void setAnswers(Map<Integer, String> answers) { this.answers = answers; }
    }

    /**
     * Returned ONLY after authoritative server-side grading.
     * Contains score, percentage, submitted answers, and full questions with explanations.
     */
    public static class QuizSubmitResponse {
        private Long quizId;
        private int score;
        private int totalQuestions;
        private int percentage;
        private List<QuizQuestionDTO> questions; // Explanations & correct answers revealed ONLY after submission
        private Map<Integer, String> submittedAnswers;

        public QuizSubmitResponse() {}

        public QuizSubmitResponse(Long quizId, int score, int totalQuestions, int percentage,
                                  List<QuizQuestionDTO> questions, Map<Integer, String> submittedAnswers) {
            this.quizId = quizId;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.percentage = percentage;
            this.questions = questions;
            this.submittedAnswers = submittedAnswers;
        }

        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public int getPercentage() { return percentage; }
        public void setPercentage(int percentage) { this.percentage = percentage; }
        public List<QuizQuestionDTO> getQuestions() { return questions; }
        public void setQuestions(List<QuizQuestionDTO> questions) { this.questions = questions; }
        public Map<Integer, String> getSubmittedAnswers() { return submittedAnswers; }
        public void setSubmittedAnswers(Map<Integer, String> submittedAnswers) { this.submittedAnswers = submittedAnswers; }
    }

    // ==========================================
    // 3. DOUBT SOLVER DTOS
    // ==========================================
    public static class DoubtRequest {
        @NotBlank(message = "Academic doubt question cannot be empty")
        private String question;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }

    public static class DoubtResponse {
        private Long doubtId;
        private String question;
        private String answer;
        private String createdAt;

        public DoubtResponse() {}

        public DoubtResponse(Long doubtId, String question, String answer, String createdAt) {
            this.doubtId = doubtId;
            this.question = question;
            this.answer = answer;
            this.createdAt = createdAt;
        }

        public Long getDoubtId() { return doubtId; }
        public void setDoubtId(Long doubtId) { this.doubtId = doubtId; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // ==========================================
    // 4. DASHBOARD STATS & RECENT ACTIVITIES
    // ==========================================
    public static class DashboardStatsResponse {
        private long totalSummaries;
        private long totalQuizzes;
        private int averageQuizScore;
        private long totalDoubts;

        public DashboardStatsResponse() {}

        public DashboardStatsResponse(long totalSummaries, long totalQuizzes, int averageQuizScore, long totalDoubts) {
            this.totalSummaries = totalSummaries;
            this.totalQuizzes = totalQuizzes;
            this.averageQuizScore = averageQuizScore;
            this.totalDoubts = totalDoubts;
        }

        public long getTotalSummaries() { return totalSummaries; }
        public void setTotalSummaries(long totalSummaries) { this.totalSummaries = totalSummaries; }
        public long getTotalQuizzes() { return totalQuizzes; }
        public void setTotalQuizzes(long totalQuizzes) { this.totalQuizzes = totalQuizzes; }
        public int getAverageQuizScore() { return averageQuizScore; }
        public void setAverageQuizScore(int averageQuizScore) { this.averageQuizScore = averageQuizScore; }
        public long getTotalDoubts() { return totalDoubts; }
        public void setTotalDoubts(long totalDoubts) { this.totalDoubts = totalDoubts; }
    }

    public static class RecentActivityDTO {
        private String type; // "NOTE", "QUIZ", "DOUBT"
        private Long id;
        private String title;
        private String detail;
        private String timestamp;

        public RecentActivityDTO() {}

        public RecentActivityDTO(String type, Long id, String title, String detail, String timestamp) {
            this.type = type;
            this.id = id;
            this.title = title;
            this.detail = detail;
            this.timestamp = timestamp;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
