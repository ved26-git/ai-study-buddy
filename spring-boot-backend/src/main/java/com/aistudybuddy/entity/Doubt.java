package com.aistudybuddy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doubts")
public class Doubt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doubt_id")
    private Long doubtId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Lob
    @Column(name = "question", nullable = false, columnDefinition = "LONGTEXT")
    private String question;

    @Lob
    @Column(name = "answer", nullable = false, columnDefinition = "LONGTEXT")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Doubt() {}

    public Doubt(User user, String question, String answer) {
        this.user = user;
        this.question = question;
        this.answer = answer;
        this.createdAt = LocalDateTime.now();
    }

    public Long getDoubtId() { return doubtId; }
    public void setDoubtId(Long doubtId) { this.doubtId = doubtId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
