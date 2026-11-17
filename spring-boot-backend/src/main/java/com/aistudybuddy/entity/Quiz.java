package com.aistudybuddy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long quizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "topic", nullable = false, length = 255)
    private String topic;

    @Column(name = "difficulty", length = 50)
    private String difficulty = "Medium";

    @Lob
    @Column(name = "questions", nullable = false, columnDefinition = "LONGTEXT")
    private String questions; // Stored as JSON string

    @Column(name = "score")
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 5;

    @Lob
    @Column(name = "submitted_answers", columnDefinition = "LONGTEXT")
    private String submittedAnswers; // Stored as JSON string

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    public Quiz() {}

    public Quiz(User user, String topic, String difficulty, String questions, Integer totalQuestions) {
        this.user = user;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questions = questions;
        this.totalQuestions = totalQuestions;
        this.createdAt = LocalDateTime.now();
    }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getQuestions() { return questions; }
    public void setQuestions(String questions) { this.questions = questions; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public String getSubmittedAnswers() { return submittedAnswers; }
    public void setSubmittedAnswers(String submittedAnswers) { this.submittedAnswers = submittedAnswers; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
