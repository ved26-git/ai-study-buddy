/**
 * AI Study Buddy - Main Application Logic (JavaScript)
 * Controls DOM manipulation, event listeners, view navigation, and interactive workflows.
 */

// Sample Notes data
const SAMPLES = {
  osi: {
    title: 'Computer Networks: 7-Layer OSI Model',
    content: `The Open Systems Interconnection (OSI) model describes seven layers that computer systems use to communicate over a network:
1. Physical Layer: Transmits raw unstructured bit stream over a physical link (cables, radio frequency, connectors).
2. Data Link Layer: Directly connected node-to-node data transfer and frame packaging (Ethernet, MAC addresses, switches).
3. Network Layer: Responsible for packet routing and forwarding through intermediate routers across different networks (IPv4/IPv6 addresses, routers).
4. Transport Layer: Manages end-to-end traffic flow control, segmentation, and reliability. Key protocols: TCP (connection-oriented, reliable) and UDP (connectionless, low-latency).
5. Session Layer: Establishes, maintains, and terminates communication sessions between software applications (RPC, SQL sessions).
6. Presentation Layer: Formats, encrypts, and compresses data for the application layer (TLS/SSL, JPEG, ASCII).
7. Application Layer: Human-computer interaction layer where applications access network services (HTTP, HTTPS, DNS, SMTP).`,
  },
  deadlock: {
    title: 'Operating Systems: Deadlock & Prevention',
    content: `A deadlock in operating systems occurs when two or more processes cannot proceed because each is holding a resource and waiting for another resource acquired by some other process.
Four Coffman conditions must hold simultaneously for a deadlock to occur:
1. Mutual Exclusion: At least one resource must be held in a non-shareable mode.
2. Hold and Wait: A process is holding at least one resource and requesting additional resources held by other processes.
3. No Preemption: Resources cannot be forcibly taken away; they are released only voluntarily by the holding process.
4. Circular Wait: A closed loop chain of processes exists, where each process holds resources needed by the next.

Deadlock Prevention involves invalidating at least one of these four conditions. Dijkstra's Banker's Algorithm is used for deadlock avoidance in multi-resource environments.`,
  },
  bio: {
    title: 'Biology: Cellular Respiration & ATP Synthesis',
    content: `Cellular respiration is a series of chemical reactions that break down glucose to produce ATP (adenosine triphosphate), powering biological functions.
The complete oxidation of one glucose molecule yields 30 to 32 ATP across three main stages:
1. Glycolysis: Occurs in the cytoplasm anaerobically. Converts 1 Glucose (6C) into 2 Pyruvate (3C), producing a net of 2 ATP and 2 NADH.
2. The Krebs Cycle (Citric Acid Cycle): Occurs in the mitochondrial matrix. Pyruvate is oxidized into Acetyl CoA, producing CO2, 2 ATP, 6 NADH, and 2 FADH2 per glucose.
3. Oxidative Phosphorylation & Electron Transport Chain (ETC): Occurs on the inner mitochondrial membrane. NADH and FADH2 donate electrons to the ETC, generating an electrochemical proton gradient. ATP Synthase uses this proton-motive force to synthesize 26 to 28 ATP through chemiosmosis. Oxygen serves as the final electron acceptor, forming water.`,
  },
};

// Complete Spring Boot files definition for the Architecture Hub
const JAVA_FILES = [
  {
    name: 'QuizController.java',
    category: 'Controller',
    path: 'src/main/java/com/aistudybuddy/controller/QuizController.java',
    description: 'REST Controller with endpoints for generating quizzes and server-authoritative grading via Spring Security principal.',
    code: `package com.aistudybuddy.controller;

import com.aistudybuddy.dto.StudyDTOs.*;
import com.aistudybuddy.entity.Quiz;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import com.aistudybuddy.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;
    private final AuthService authService;

    public QuizController(QuizService quizService, AuthService authService) {
        this.quizService = quizService;
        this.authService = authService;
    }

    @PostMapping("/generate")
    public ResponseEntity<QuizAttemptResponse> generateQuiz(@Valid @RequestBody QuizGenerateRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        // Server conceals answer keys from public response
        QuizAttemptResponse quiz = quizService.generateQuiz(user, request);
        return new ResponseEntity<>(quiz, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<QuizSubmitResponse> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody QuizSubmitRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        // Score is calculated authoritatively on the server!
        QuizSubmitResponse response = quizService.submitQuiz(id, user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<QuizListResponse>> getUserQuizzes() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(quizService.getUserQuizzes(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuizById(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(quizService.getQuizDetailsSafe(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        quizService.deleteQuiz(id, user);
        return ResponseEntity.ok(Map.of("message", "Quiz deleted successfully"));
    }
}`,
  },
  {
    name: 'GeminiService.java',
    category: 'Service',
    path: 'src/main/java/com/aistudybuddy/service/GeminiService.java',
    description: 'Spring service communicating with Google Gemini API for notes summary, MCQs, and doubts.',
    code: `package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.QuizQuestionDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class GeminiService {

    @Value("\${gemini.api.key:}")
    private String apiKey;

    @Value("\${gemini.api.model:gemini-3.8-flash}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String summarizeNotes(String title, String content) {
        String systemInstruction = "You are an expert AI academic tutor. Summarize the provided study notes into simple, "
                + "easy-to-understand language. Identify important concepts, key points, and definitions. "
                + "Use headings and bullet points. Preserve important information without changing its meaning.";

        String userPrompt = "Title: " + (title != null ? title : "Study Notes") + "\\n\\nContent:\\n" + content;
        return callGemini(systemInstruction, userPrompt);
    }

    public List<QuizQuestionDTO> generateQuiz(String topic, int numQuestions, String difficulty) {
        String systemInstruction = "You are an academic examiner. Return a JSON object with a 'questions' array. "
                + "Each item must have: 'question' (string), 'options' (array of 4 strings), 'correctAnswer' (string matching one option), 'explanation' (string).";

        String userPrompt = String.format("Generate %d %s multiple-choice questions on: '%s'", numQuestions, difficulty, topic);
        String jsonResponse = callGemini(systemInstruction, userPrompt);
        return parseQuizQuestions(jsonResponse);
    }

    public String solveDoubt(String question) {
        String systemInstruction = "You are an encouraging academic tutor. Provide clear, student-friendly, "
                + "step-by-step explanations with analogies and examples.";
        return callGemini(systemInstruction, question);
    }

    private String callGemini(String systemInstruction, String prompt) {
        // Connects to Google Gemini API endpoint with gemini-3.8-flash
        ...
    }
}`,
  },
  {
    name: 'NotesController.java',
    category: 'Controller',
    path: 'src/main/java/com/aistudybuddy/controller/NotesController.java',
    description: 'REST Controller managing study notes summarization and CRUD operations.',
    code: `package com.aistudybuddy.controller;

import com.aistudybuddy.dto.StudyDTOs.NoteResponse;
import com.aistudybuddy.dto.StudyDTOs.SummarizeRequest;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import com.aistudybuddy.service.NotesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*")
public class NotesController {

    private final NotesService notesService;
    private final AuthService authService;

    public NotesController(NotesService notesService, AuthService authService) {
        this.notesService = notesService;
        this.authService = authService;
    }

    @PostMapping("/summarize")
    public ResponseEntity<NoteResponse> summarizeNotes(
            @RequestHeader(value = "X-User-Email", defaultValue = "alex@aistudybuddy.com") String email,
            @Valid @RequestBody SummarizeRequest request) {
        User user = authService.getAuthenticatedUser(email);
        NoteResponse response = notesService.summarizeAndSave(user, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getNotes(
            @RequestHeader(value = "X-User-Email", defaultValue = "alex@aistudybuddy.com") String email) {
        User user = authService.getAuthenticatedUser(email);
        return ResponseEntity.ok(notesService.getUserNotes(user));
    }
}`,
  },
  {
    name: 'Quiz.java',
    category: 'Entity',
    path: 'src/main/java/com/aistudybuddy/entity/Quiz.java',
    description: 'JPA Entity mapped to MySQL `quizzes` table with score tracking and user foreign key.',
    code: `package com.aistudybuddy.entity;

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
    private String questions;

    @Column(name = "score")
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 5;

    @Lob
    @Column(name = "submitted_answers", columnDefinition = "LONGTEXT")
    private String submittedAnswers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors, Getters & Setters...
}`,
  },
  {
    name: 'StudyNotes.java',
    category: 'Entity',
    path: 'src/main/java/com/aistudybuddy/entity/StudyNotes.java',
    description: 'JPA Entity mapped to MySQL `study_notes` table with LONGTEXT original and summary.',
    code: `package com.aistudybuddy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_notes")
public class StudyNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "original_content", nullable = false, columnDefinition = "LONGTEXT")
    private String originalContent;

    @Lob
    @Column(name = "summary", nullable = false, columnDefinition = "LONGTEXT")
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors, Getters & Setters...
}`,
  },
  {
    name: 'User.java',
    category: 'Entity',
    path: 'src/main/java/com/aistudybuddy/entity/User.java',
    description: 'JPA Entity mapped to MySQL `users` table with BCrypt encrypted passwords.',
    code: `package com.aistudybuddy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @Lob
    @Column(name = "profile_picture", columnDefinition = "LONGTEXT")
    private String profilePicture;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyNotes> studyNotes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quiz> quizzes = new ArrayList<>();

    // Constructors, Getters & Setters...
}`,
  },
  {
    name: 'SecurityConfig.java',
    category: 'Security',
    path: 'src/main/java/com/aistudybuddy/security/SecurityConfig.java',
    description: 'Spring Security 6 configuration with BCryptPasswordEncoder and JWT stateless filter.',
    code: `package com.aistudybuddy.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/error").permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}`,
  },
  {
    name: 'schema.sql',
    category: 'Database',
    path: 'src/main/resources/schema.sql',
    description: 'MySQL database creation script with users, study_notes, quizzes, and foreign keys.',
    code: `-- AI Study Buddy MySQL Schema
CREATE DATABASE IF NOT EXISTS ai_study_buddy
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_study_buddy;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS study_notes (
    note_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    original_content LONGTEXT NOT NULL,
    summary LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_studynotes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quizzes (
    quiz_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) DEFAULT 'Medium',
    questions LONGTEXT NOT NULL,
    score INT DEFAULT NULL,
    total_questions INT NOT NULL DEFAULT 5,
    submitted_answers LONGTEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_quiz_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;`,
  },
  {
    name: 'pom.xml',
    category: 'Config',
    path: 'pom.xml',
    description: 'Maven dependencies: Spring Boot Starter Web, Data JPA, Security, MySQL Connector, JJWT.',
    code: `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.4</version>
        <relativePath/>
    </parent>
    <groupId>com.aistudybuddy</groupId>
    <artifactId>ai-study-buddy</artifactId>
    <version>1.0.0</version>
    <name>AI Study Buddy</name>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>`,
  },
  {
    name: 'Doubt.java',
    category: 'Entity',
    path: 'src/main/java/com/aistudybuddy/entity/Doubt.java',
    description: 'JPA Entity mapped to MySQL `doubts` table storing student questions and Gemini AI answers.',
    code: `package com.aistudybuddy.entity;

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

    // Constructors, Getters & Setters...
}`,
  },
  {
    name: 'DoubtSolverController.java',
    category: 'Controller',
    path: 'src/main/java/com/aistudybuddy/controller/DoubtSolverController.java',
    description: 'REST Controller for asking academic doubts, retrieving chat history, and clearing conversations.',
    code: `package com.aistudybuddy.controller;

import com.aistudybuddy.dto.StudyDTOs.DoubtRequest;
import com.aistudybuddy.dto.StudyDTOs.DoubtResponse;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import com.aistudybuddy.service.DoubtSolverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doubts")
public class DoubtSolverController {

    private final DoubtSolverService doubtSolverService;
    private final AuthService authService;

    public DoubtSolverController(DoubtSolverService doubtSolverService, AuthService authService) {
        this.doubtSolverService = doubtSolverService;
        this.authService = authService;
    }

    @PostMapping("/ask")
    public ResponseEntity<DoubtResponse> askDoubt(@Valid @RequestBody DoubtRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        DoubtResponse response = doubtSolverService.solveDoubt(user, request.getQuestion());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<DoubtResponse>> getDoubtHistory() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(doubtSolverService.getUserDoubts(user));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAllDoubts() {
        User user = authService.getCurrentAuthenticatedUser();
        doubtSolverService.clearUserDoubts(user);
        return ResponseEntity.ok(Map.of("message", "All doubt chat history cleared"));
    }
}`,
  },
  {
    name: 'AuthController.java',
    category: 'Controller',
    path: 'src/main/java/com/aistudybuddy/controller/AuthController.java',
    description: 'REST Controller handling student registration, login, profile retrieval (/me), and logout.',
    code: `package com.aistudybuddy.controller;

import com.aistudybuddy.dto.AuthDTOs.*;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummary> getCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new UserSummary(user.getUserId(), user.getName(), user.getEmail(), user.getProfilePicture()));
    }
}`,
  },
];

class AppUI {
  constructor() {
    this.currentView = 'dashboard';
    this.activeQuiz = null;
    this.selectedJavaFile = JAVA_FILES[0];
    this.toastInstance = null;
    this.selectedAuthAvatar = '/assets/images/student_avatar_user_1794203529926.jpg';
    this.lineChartRoot = null;
    this.barChartRoot = null;
    this.init();
  }

  init() {
    const toastEl = document.getElementById('appToast');
    if (toastEl) {
      this.toastInstance = new bootstrap.Toast(toastEl, { delay: 3500 });
    }

    this.updateModeUI(window.apiService.getMode());
    this.bindEvents();
    this.initUser();
    this.renderJavaFilesList();
    this.renderJavaFileDetail();
    this.refreshDashboard();
    this.loadAppConfig();
  }

  async loadAppConfig() {
    try {
      const cfg = await window.apiService.getAppConfig();
      if (cfg && cfg.geminiModel) {
        const label = document.getElementById('activeGeminiModelLabel');
        if (label) {
          label.textContent = cfg.geminiModel;
        }
      }
    } catch (e) {
      console.warn('Could not load app config:', e);
    }
  }

  updateModeUI(mode) {
    const label = document.getElementById('backendModeLabel');
    const icon = document.getElementById('backendModeIcon');
    const btnLive = document.getElementById('setLiveBackend');
    const btnDemo = document.getElementById('setDemoBackend');

    if (mode === 'live') {
      if (label) label.textContent = 'Mode: Spring Boot (Live)';
      if (icon) icon.className = 'bi bi-hdd-network text-success';
      if (btnLive) btnLive.classList.add('active');
      if (btnDemo) btnDemo.classList.remove('active');
    } else {
      if (label) label.textContent = 'Mode: Demo Preview (Isolated)';
      if (icon) icon.className = 'bi bi-browser-chrome text-warning';
      if (btnDemo) btnDemo.classList.add('active');
      if (btnLive) btnLive.classList.remove('active');
    }
  }

  showToast(message, type = 'success') {
    const toastBody = document.getElementById('toastMessage');
    const toastEl = document.getElementById('appToast');
    if (!toastBody || !this.toastInstance) return;

    let icon = '<i class="bi bi-check-circle-fill text-success fs-5"></i>';
    if (type === 'error') {
      icon = '<i class="bi bi-exclamation-triangle-fill text-danger fs-5"></i>';
    } else if (type === 'info') {
      icon = '<i class="bi bi-info-circle-fill text-primary fs-5"></i>';
    }

    toastBody.innerHTML = `${icon} <span class="fw-semibold text-dark">${message}</span>`;
    this.toastInstance.show();
  }

  initUser() {
    let user = window.apiService.getCurrentUser();
    const defaultAvatar = '/assets/images/student_avatar_user_1794203529926.jpg';
    if (!user) {
      // Default to demo student for effortless immediate interactivity
      user = {
        user_id: 1,
        name: 'Alex Johnson',
        email: 'alex@aistudybuddy.com',
        profile_picture: defaultAvatar,
      };
      window.apiService.setCurrentUser(user);
    } else if (!user.profile_picture && !user.profilePicture) {
      user.profile_picture = defaultAvatar;
      window.apiService.setCurrentUser(user);
    }
    this.updateUserUI(user);
  }

  updateUserUI(user) {
    const defaultAvatar = '/assets/images/student_avatar_user_1794203529926.jpg';
    if (user) {
      const avatarUrl = user.profile_picture || user.profilePicture || defaultAvatar;
      const initial = user.name ? user.name.charAt(0).toUpperCase() : 'S';

      document.getElementById('userDisplayName').textContent = user.name || 'Student';
      document.getElementById('userDisplayEmail').textContent = user.email || '';
      document.getElementById('dashWelcomeName').textContent = user.name ? user.name.split(' ')[0] : 'Student';

      // Sidebar Avatar
      const userAvatarImg = document.getElementById('userAvatarImg');
      const userAvatarInitial = document.getElementById('userAvatarInitial');
      if (userAvatarImg && userAvatarInitial) {
        if (avatarUrl) {
          userAvatarImg.src = avatarUrl;
          userAvatarImg.classList.remove('d-none');
          userAvatarInitial.classList.add('d-none');
          userAvatarImg.onerror = () => {
            userAvatarImg.classList.add('d-none');
            userAvatarInitial.classList.remove('d-none');
          };
        } else {
          userAvatarImg.classList.add('d-none');
          userAvatarInitial.classList.remove('d-none');
          userAvatarInitial.textContent = initial;
        }
      }

      // TopBar Avatar
      const topBarAvatarImg = document.getElementById('topBarAvatarImg');
      const topBarAvatarIcon = document.getElementById('topBarAvatarIcon');
      if (topBarAvatarImg && topBarAvatarIcon) {
        if (avatarUrl) {
          topBarAvatarImg.src = avatarUrl;
          topBarAvatarImg.classList.remove('d-none');
          topBarAvatarIcon.classList.add('d-none');
          topBarAvatarImg.onerror = () => {
            topBarAvatarImg.classList.add('d-none');
            topBarAvatarIcon.classList.remove('d-none');
          };
        } else {
          topBarAvatarImg.classList.add('d-none');
          topBarAvatarIcon.classList.remove('d-none');
        }
      }

      // Profile Modal Avatar
      document.getElementById('profileModalName').textContent = user.name || 'Student';
      document.getElementById('profileModalEmail').textContent = user.email || '';
      const profileModalImg = document.getElementById('profileModalImg');
      const profileModalInitial = document.getElementById('profileModalInitial');
      if (profileModalImg && profileModalInitial) {
        if (avatarUrl) {
          profileModalImg.src = avatarUrl;
          profileModalImg.classList.remove('d-none');
          profileModalInitial.classList.add('d-none');
          profileModalImg.onerror = () => {
            profileModalImg.classList.add('d-none');
            profileModalInitial.classList.remove('d-none');
          };
        } else {
          profileModalImg.classList.add('d-none');
          profileModalInitial.classList.remove('d-none');
          profileModalInitial.textContent = initial;
        }
      }
    } else {
      document.getElementById('userDisplayName').textContent = 'Guest Student';
      document.getElementById('userDisplayEmail').textContent = 'Not logged in';
      document.getElementById('dashWelcomeName').textContent = 'Student';

      const userAvatarImg = document.getElementById('userAvatarImg');
      const userAvatarInitial = document.getElementById('userAvatarInitial');
      if (userAvatarImg && userAvatarInitial) {
        userAvatarImg.classList.add('d-none');
        userAvatarInitial.classList.remove('d-none');
        userAvatarInitial.textContent = 'G';
      }

      const topBarAvatarImg = document.getElementById('topBarAvatarImg');
      const topBarAvatarIcon = document.getElementById('topBarAvatarIcon');
      if (topBarAvatarImg && topBarAvatarIcon) {
        topBarAvatarImg.classList.add('d-none');
        topBarAvatarIcon.classList.remove('d-none');
      }
    }
  }

  bindEvents() {
    // Navigation items
    document.querySelectorAll('.nav-link-custom').forEach((link) => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const view = link.dataset.view;
        this.switchView(view);

        // Auto close mobile sidebar
        const sidebar = document.getElementById('sidebar');
        if (sidebar.classList.contains('show')) {
          sidebar.classList.remove('show');
        }
      });
    });

    // Mobile Hamburger Toggle
    document.getElementById('mobileMenuToggle').addEventListener('click', () => {
      document.getElementById('sidebar').classList.toggle('show');
    });

    // Quick Action button in TopBar
    document.getElementById('quickSummarizeBtn').addEventListener('click', () => {
      this.switchView('notes');
    });

    // Hero buttons
    document.getElementById('heroSummarizeBtn').addEventListener('click', () => this.switchView('notes'));
    document.getElementById('heroQuizBtn').addEventListener('click', () => this.switchView('quiz'));
    document.getElementById('heroDoubtBtn').addEventListener('click', () => this.switchView('doubts'));

    // Dashboard Tool Cards
    document.getElementById('cardNavNotes').addEventListener('click', () => this.switchView('notes'));
    document.getElementById('cardNavQuiz').addEventListener('click', () => this.switchView('quiz'));
    document.getElementById('cardNavDoubts').addEventListener('click', () => this.switchView('doubts'));
    document.getElementById('viewAllHistoryBtn').addEventListener('click', () => this.switchView('history'));

    // Dashboard Stats Refresh Button (triggers AJAX call to backend)
    const refreshStatsBtn = document.getElementById('refreshDashboardBtn');
    if (refreshStatsBtn) {
      refreshStatsBtn.addEventListener('click', () => this.handleManualStatsRefresh());
    }

    // PDF Performance Report Generator Buttons
    const generatePdfBtn = document.getElementById('generatePdfReportBtn');
    if (generatePdfBtn) {
      generatePdfBtn.addEventListener('click', () => this.openPdfReportModal());
    }

    const historyPdfBtn = document.getElementById('historyPdfReportBtn');
    if (historyPdfBtn) {
      historyPdfBtn.addEventListener('click', () => this.openPdfReportModal());
    }

    // PDF Report Modal Actions (Download & Print)
    const downloadPdfFileBtn = document.getElementById('downloadPdfFileBtn');
    if (downloadPdfFileBtn) {
      downloadPdfFileBtn.addEventListener('click', () => this.downloadPdfReport());
    }

    const downloadPdfFooterBtn = document.getElementById('downloadPdfFooterBtn');
    if (downloadPdfFooterBtn) {
      downloadPdfFooterBtn.addEventListener('click', () => this.downloadPdfReport());
    }

    const printReportBtn = document.getElementById('printReportBtn');
    if (printReportBtn) {
      printReportBtn.addEventListener('click', () => this.printPdfReport());
    }

    const printReportFooterBtn = document.getElementById('printReportFooterBtn');
    if (printReportFooterBtn) {
      printReportFooterBtn.addEventListener('click', () => this.printPdfReport());
    }

    // Backend Mode Switcher
    document.getElementById('setLiveBackend').addEventListener('click', (e) => {
      e.preventDefault();
      window.apiService.setMode('live');
      this.updateModeUI('live');
      this.showToast('Backend switched to Local Spring Boot REST API (localhost:8080)!', 'info');
      this.refreshDashboard();
    });

    document.getElementById('setDemoBackend').addEventListener('click', (e) => {
      e.preventDefault();
      window.apiService.setMode('demo');
      this.updateModeUI('demo');
      this.showToast('Backend switched to In-Browser Demo mode (isolated preview).', 'warning');
      this.refreshDashboard();
    });

    // Notes Summarizer Form & Character Counter
    const noteContent = document.getElementById('noteContentInput');
    noteContent.addEventListener('input', () => {
      document.getElementById('notesCharCount').textContent = `${noteContent.value.length} characters`;
    });

    document.getElementById('notesForm').addEventListener('submit', (e) => this.handleNotesSummarize(e));

    // Sample Notes loader
    document.getElementById('sampleNotesOSI').addEventListener('click', () => this.loadSampleNotes('osi'));
    document.getElementById('sampleNotesDeadlock').addEventListener('click', () => this.loadSampleNotes('deadlock'));
    document.getElementById('sampleNotesBio').addEventListener('click', () => this.loadSampleNotes('bio'));

    // Copy Summary Button
    document.getElementById('copySummaryBtn').addEventListener('click', () => this.copySummaryContent());
    document.getElementById('summarizeToQuizBtn').addEventListener('click', () => {
      const title = document.getElementById('summaryResultTitle').textContent;
      document.getElementById('quizTopicInput').value = title;
      this.switchView('quiz');
    });

    // Quiz Config & Generation
    document.getElementById('quizGenForm').addEventListener('submit', (e) => this.handleGenerateQuiz(e));

    document.querySelectorAll('.quick-topic-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        document.getElementById('quizTopicInput').value = btn.textContent.trim();
      });
    });

    // Quiz Answer Submission
    document.getElementById('quizAnswerForm').addEventListener('submit', (e) => this.handleSubmitQuiz(e));
    document.getElementById('resetQuizBtn').addEventListener('click', () => this.resetQuizState());
    document.getElementById('retakeQuizBtn').addEventListener('click', () => this.resetQuizState());

    // Doubt Solver Form & Quick Prompts
    document.getElementById('doubtChatForm').addEventListener('submit', (e) => this.handleSendDoubt(e));
    document.getElementById('clearChatBtn').addEventListener('click', () => this.clearChatHistory());

    document.querySelectorAll('.quick-doubt-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        document.getElementById('doubtInput').value = btn.textContent.trim();
        this.handleSendDoubt();
      });
    });

    // History Filters
    document.querySelectorAll('.history-filter-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.history-filter-btn').forEach((b) => b.classList.remove('active'));
        btn.classList.add('active');
        this.renderHistoryList();
      });
    });

    document.getElementById('historySearchInput').addEventListener('input', () => this.renderHistoryList());

    // Profile & Auth Modals
    const openProfileOrAuth = () => {
      const user = window.apiService.getCurrentUser();
      if (user) {
        new bootstrap.Modal(document.getElementById('profileModal')).show();
      } else {
        new bootstrap.Modal(document.getElementById('authModal')).show();
      }
    };

    document.getElementById('profileModalTrigger').addEventListener('click', openProfileOrAuth);
    const sidebarProfileClick = document.getElementById('sidebarProfileClick');
    if (sidebarProfileClick) {
      sidebarProfileClick.addEventListener('click', openProfileOrAuth);
    }

    document.getElementById('logoutBtn').addEventListener('click', () => this.handleLogout());
    document.getElementById('profileSignOutBtn').addEventListener('click', () => {
      bootstrap.Modal.getInstance(document.getElementById('profileModal')).hide();
      this.handleLogout();
    });

    // Auth Mode Radios
    document.querySelectorAll('input[name="authModeRadio"]').forEach((radio) => {
      radio.addEventListener('change', (e) => {
        const isRegister = e.target.value === 'register';
        document.getElementById('authNameGroup').classList.toggle('d-none', !isRegister);
        document.getElementById('authAvatarGroup').classList.toggle('d-none', !isRegister);
        document.getElementById('authSubmitBtn').textContent = isRegister ? 'Create Account' : 'Sign In';
        document.getElementById('authModalTitle').textContent = isRegister ? 'Create Student Account' : 'Sign in to AI Study Buddy';
      });
    });

    // Preset Avatar Selection in Register Modal
    document.querySelectorAll('#authAvatarGroup .avatar-preset-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        const url = btn.dataset.avatarUrl;
        if (url) {
          this.selectedAuthAvatar = url;
          const preview = document.getElementById('authAvatarPreview');
          if (preview) preview.src = url;

          document.querySelectorAll('#authAvatarGroup .avatar-preset-btn').forEach((b) => b.classList.remove('selected'));
          btn.classList.add('selected');
        }
      });
    });

    // Custom File Upload in Register Modal
    const authAvatarUploadBtn = document.getElementById('authAvatarUploadBtn');
    const authAvatarFileInput = document.getElementById('authAvatarFileInput');
    if (authAvatarUploadBtn && authAvatarFileInput) {
      authAvatarUploadBtn.addEventListener('click', () => authAvatarFileInput.click());
      authAvatarFileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
          const reader = new FileReader();
          reader.onload = (loadEvt) => {
            const dataUrl = loadEvt.target.result;
            this.selectedAuthAvatar = dataUrl;
            const preview = document.getElementById('authAvatarPreview');
            if (preview) preview.src = dataUrl;
            document.querySelectorAll('#authAvatarGroup .avatar-preset-btn').forEach((b) => b.classList.remove('selected'));
          };
          reader.readAsDataURL(file);
        }
      });
    }

    // Profile Modal Avatar Controls
    const profileToggleChangeAvatarBtn = document.getElementById('profileToggleChangeAvatarBtn');
    const profileAvatarPickerSection = document.getElementById('profileAvatarPickerSection');
    const closeProfileAvatarPickerBtn = document.getElementById('closeProfileAvatarPickerBtn');
    if (profileToggleChangeAvatarBtn && profileAvatarPickerSection) {
      profileToggleChangeAvatarBtn.addEventListener('click', () => {
        profileAvatarPickerSection.classList.toggle('d-none');
      });
    }
    if (closeProfileAvatarPickerBtn && profileAvatarPickerSection) {
      closeProfileAvatarPickerBtn.addEventListener('click', () => {
        profileAvatarPickerSection.classList.add('d-none');
      });
    }

    // Preset Selection in Profile Modal
    document.querySelectorAll('.profile-preset-btn').forEach((btn) => {
      btn.addEventListener('click', async () => {
        const url = btn.dataset.avatarUrl;
        if (url) {
          await window.apiService.updateProfile(null, url);
          this.updateUserUI(window.apiService.getCurrentUser());
          if (profileAvatarPickerSection) profileAvatarPickerSection.classList.add('d-none');
          this.showToast('Profile picture updated successfully!', 'success');
        }
      });
    });

    // Custom Upload in Profile Modal
    const profileUploadCustomBtn = document.getElementById('profileUploadCustomBtn');
    const profileAvatarFileInput = document.getElementById('profileAvatarFileInput');
    if (profileUploadCustomBtn && profileAvatarFileInput) {
      profileUploadCustomBtn.addEventListener('click', () => profileAvatarFileInput.click());
      profileAvatarFileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
          const reader = new FileReader();
          reader.onload = async (loadEvt) => {
            const dataUrl = loadEvt.target.result;
            await window.apiService.updateProfile(null, dataUrl);
            this.updateUserUI(window.apiService.getCurrentUser());
            if (profileAvatarPickerSection) profileAvatarPickerSection.classList.add('d-none');
            this.showToast('Profile picture updated successfully!', 'success');
          };
          reader.readAsDataURL(file);
        }
      });
    }

    document.getElementById('authForm').addEventListener('submit', (e) => this.handleAuthSubmit(e));
    document.getElementById('quickDemoStudentBtn').addEventListener('click', () => this.handleQuickDemoLogin());

    // Java Hub Copy & Download
    document.getElementById('copyJavaCodeBtn').addEventListener('click', () => this.copySelectedJavaCode());
    document.getElementById('downloadJavaFileBtn').addEventListener('click', () => this.downloadSelectedJavaFile());
  }

  switchView(viewName) {
    this.currentView = viewName;
    document.querySelectorAll('.app-view').forEach((el) => el.classList.add('d-none'));
    document.querySelectorAll('.nav-link-custom').forEach((el) => el.classList.remove('active'));

    const targetView = document.getElementById(`view-${viewName}`);
    if (targetView) targetView.classList.remove('d-none');

    const activeNav = document.querySelector(`.nav-link-custom[data-view="${viewName}"]`);
    if (activeNav) activeNav.classList.add('active');

    const titles = {
      dashboard: 'Dashboard',
      notes: 'AI Notes Summarizer',
      quiz: 'AI Quiz Generator',
      doubts: 'AI Doubt Solver',
      history: 'Learning History',
      'spring-boot': 'Java Spring Boot Hub',
    };
    document.getElementById('currentViewTitle').textContent = titles[viewName] || 'Dashboard';

    if (viewName === 'dashboard') this.refreshDashboard();
    if (viewName === 'history') this.renderHistoryList();
    if (viewName === 'doubts') this.renderChatMessages();
  }

  // --- Dashboard Logic ---
  async handleManualStatsRefresh() {
    const icon = document.getElementById('refreshDashboardIcon');
    const btn = document.getElementById('refreshDashboardBtn');
    if (btn) btn.disabled = true;
    if (icon) icon.classList.add('spin-animation');

    try {
      await this.refreshDashboard(true);
      this.showToast('Statistics updated with latest backend data!', 'success');
    } catch (err) {
      console.error('Failed to refresh stats:', err);
      this.showToast(err.message || 'Failed to refresh statistics from backend', 'error');
    } finally {
      if (icon) icon.classList.remove('spin-animation');
      if (btn) btn.disabled = false;
    }
  }

  async refreshDashboard(rethrow = false) {
    try {
      const stats = await window.apiService.getDashboardStats();
      const rawAvg = stats?.averageQuizScore ?? stats?.average_quiz_score ?? 0;
      const avgScore = (typeof rawAvg === 'number' && Number.isFinite(rawAvg)) ? Math.max(0, Math.min(100, Math.round(rawAvg))) : 0;
      const summariesCount = stats?.totalSummaries ?? stats?.total_summaries ?? 0;
      const quizzesCount = stats?.totalQuizzes ?? stats?.total_quizzes ?? 0;

      document.getElementById('dashTotalSummaries').textContent = summariesCount;
      document.getElementById('dashTotalQuizzes').textContent = quizzesCount;
      document.getElementById('dashAvgScore').textContent = `${avgScore}%`;

      document.getElementById('profileSummariesCount').textContent = summariesCount;
      document.getElementById('profileQuizzesCount').textContent = quizzesCount;
      document.getElementById('profileAvgScore').textContent = `${avgScore}%`;

      const history = await window.apiService.getHistory();
      this.renderRecentActivities(history);
      this.renderDashboardCharts(history, stats);
    } catch (err) {
      console.error('Error refreshing dashboard data:', err);
      if (rethrow) throw err;
    }
  }

  // --- Recharts Visual Analytics Rendering ---
  renderDashboardCharts(history, stats) {
    const lineChartContainer = document.getElementById('quizScoreLineChartContainer');
    const barChartContainer = document.getElementById('activityBreakdownBarChartContainer');
    if (!lineChartContainer || !barChartContainer) return;

    // Helper: Format precise date with weekday, full date, and 12-hour time
    const formatPreciseDate = (rawDate) => {
      if (!rawDate) return 'N/A';
      try {
        const d = new Date(rawDate);
        if (isNaN(d.getTime())) return String(rawDate);
        return d.toLocaleDateString('en-US', {
          weekday: 'short',
          month: 'short',
          day: 'numeric',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        });
      } catch (e) {
        return String(rawDate);
      }
    };

    // 1. Prepare Line Chart Data (Daily Quiz Scores)
    const quizzes = (history?.quizzes || []).filter(q => q.score !== null && (q.totalQuestions || q.total_questions));
    quizzes.sort((a, b) => {
      const dateA = new Date(a.completedAt || a.completed_at || a.createdAt || a.created_at || 0).getTime();
      const dateB = new Date(b.completedAt || b.completed_at || b.createdAt || b.created_at || 0).getTime();
      return dateA - dateB;
    });

    const lineData = quizzes.map((q, idx) => {
      const totalQ = q.totalQuestions || q.total_questions || 5;
      const score = (q.score !== null && q.score !== undefined) ? q.score : 0;
      const pct = Math.round((score / totalQ) * 100);
      const rawDate = q.completedAt || q.completed_at || q.createdAt || q.created_at || Date.now();
      const dateObj = new Date(rawDate);
      const dateLabel = isNaN(dateObj.getTime())
        ? `Quiz ${idx + 1}`
        : dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
      const preciseDate = formatPreciseDate(rawDate);

      const diffRaw = q.difficulty ? String(q.difficulty).trim() : 'Medium';
      const formattedDiff = diffRaw.charAt(0).toUpperCase() + diffRaw.slice(1).toLowerCase();

      return {
        date: dateLabel,
        score: pct,
        topic: q.topic || `Quiz ${idx + 1}`,
        rawScore: `${score} / ${totalQ}`,
        preciseDate: preciseDate,
        activityType: 'AI Practice Quiz',
        difficulty: formattedDiff,
        passed: pct >= 70,
      };
    });

    // 2. Prepare Bar Chart Data (Activity Types Comparison)
    const rawAvg = stats?.averageQuizScore ?? stats?.average_quiz_score ?? 0;
    const avgScore = (typeof rawAvg === 'number' && Number.isFinite(rawAvg)) ? Math.max(0, Math.min(100, Math.round(rawAvg))) : 0;

    const summariesCount = stats?.totalSummaries ?? stats?.total_summaries ?? (history?.summaries?.length || 0);
    const quizzesCount = stats?.totalQuizzes ?? stats?.total_quizzes ?? quizzes.length;
    const doubtsCount = stats?.totalDoubts ?? stats?.total_doubts ?? (history?.doubts?.length || 0);

    // Precise timestamps for each category
    const lastSummary = (history?.summaries && history.summaries.length > 0)
      ? history.summaries[history.summaries.length - 1]
      : null;
    const summaryDateRaw = lastSummary?.createdAt || lastSummary?.created_at;
    const summaryPreciseDate = summaryDateRaw
      ? formatPreciseDate(summaryDateRaw)
      : (summariesCount > 0 ? 'Recently completed' : 'No records yet');

    const lastQuiz = (quizzes && quizzes.length > 0)
      ? quizzes[quizzes.length - 1]
      : null;
    const quizDateRaw = lastQuiz?.completedAt || lastQuiz?.completed_at || lastQuiz?.createdAt || lastQuiz?.created_at;
    const quizPreciseDate = quizDateRaw
      ? formatPreciseDate(quizDateRaw)
      : (quizzesCount > 0 ? 'Recently completed' : 'No records yet');

    const lastDoubt = (history?.doubts && history.doubts.length > 0)
      ? history.doubts[history.doubts.length - 1]
      : null;
    const doubtDateRaw = lastDoubt?.createdAt || lastDoubt?.created_at;
    const doubtPreciseDate = doubtDateRaw
      ? formatPreciseDate(doubtDateRaw)
      : (doubtsCount > 0 ? 'Recently completed' : 'No records yet');

    const barData = [
      {
        activity: 'Summaries',
        count: summariesCount,
        fill: '#7c3aed',
        label: 'Lecture Notes Summaries',
        activityType: 'Notes Summarizer',
        scoreMetric: `${summariesCount} Summaries Generated (100% Retained)`,
        preciseDate: summaryPreciseDate,
      },
      {
        activity: 'Quizzes',
        count: quizzesCount,
        fill: '#4f46e5',
        label: 'Multiple Choice Quizzes',
        activityType: 'Quiz Assessment',
        scoreMetric: `${avgScore}% Average Score (${quizzesCount} Tests)`,
        preciseDate: quizPreciseDate,
      },
      {
        activity: 'Doubts',
        count: doubtsCount,
        fill: '#059669',
        label: 'AI Doubt Tutor Sessions',
        activityType: 'Doubt Solver',
        scoreMetric: `${doubtsCount} Inquiries Resolved (100% Rate)`,
        preciseDate: doubtPreciseDate,
      },
    ];

    // 3. Render via Recharts React UMD
    // @ts-ignore
    if (window.React && window.ReactDOM && window.Recharts) {
      // @ts-ignore
      const h = window.React.createElement;
      // @ts-ignore
      const { ResponsiveContainer, LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Cell, ReferenceLine } = window.Recharts;

      // Custom Tooltip for Line Chart (Daily Quiz Score Progression)
      const CustomLineTooltip = function(props) {
        if (!props || !props.active || !props.payload || !props.payload.length) return null;
        const data = props.payload[0]?.payload;
        if (!data) return null;

        const isPassed = data.passed;
        return h('div', {
          style: {
            backgroundColor: '#ffffff',
            borderRadius: '10px',
            padding: '12px 14px',
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.14), 0 8px 10px -6px rgba(0, 0, 0, 0.08)',
            border: '1px solid #e2e8f0',
            minWidth: '230px',
            maxWidth: '300px',
            fontSize: '12px',
            fontFamily: 'inherit',
            color: '#1e293b',
            pointerEvents: 'none',
          }
        }, [
          // Header: Activity badge & pass/fail indicator
          h('div', {
            key: 'hdr',
            style: {
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '8px',
              borderBottom: '1px solid #f1f5f9',
              paddingBottom: '6px',
            }
          }, [
            h('span', {
              key: 'type-badge',
              style: {
                backgroundColor: '#eef2ff',
                color: '#4338ca',
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '4px',
                fontSize: '10.5px',
                textTransform: 'uppercase',
                letterSpacing: '0.4px',
              }
            }, data.activityType || 'AI Quiz'),
            h('span', {
              key: 'status-badge',
              style: {
                color: isPassed ? '#059669' : '#dc2626',
                fontWeight: 700,
                fontSize: '11px',
              }
            }, isPassed ? '✓ Passed (≥70%)' : '⚠ Below 70%')
          ]),

          // Topic Headline
          h('div', {
            key: 'topic-title',
            style: {
              fontWeight: 700,
              fontSize: '13px',
              color: '#0f172a',
              marginBottom: '8px',
              lineHeight: '1.3',
            }
          }, data.topic),

          // Structured Data Grid
          h('div', {
            key: 'details-grid',
            style: {
              display: 'grid',
              gridTemplateColumns: 'auto 1fr',
              gap: '5px 12px',
              fontSize: '11.5px',
              lineHeight: '1.4',
            }
          }, [
            h('span', { key: 'l-act', style: { color: '#64748b', fontWeight: 500 } }, 'Activity Type:'),
            h('span', { key: 'v-act', style: { fontWeight: 600, color: '#1e293b' } }, data.activityType),

            h('span', { key: 'l-score', style: { color: '#64748b', fontWeight: 500 } }, 'Score:'),
            h('span', { key: 'v-score', style: { fontWeight: 700, color: isPassed ? '#059669' : '#dc2626' } }, `${data.score}% (${data.rawScore} correct)`),

            h('span', { key: 'l-date', style: { color: '#64748b', fontWeight: 500 } }, 'Precise Date:'),
            h('span', { key: 'v-date', style: { fontWeight: 600, color: '#334155' } }, data.preciseDate),

            h('span', { key: 'l-diff', style: { color: '#64748b', fontWeight: 500 } }, 'Difficulty:'),
            h('span', { key: 'v-diff', style: { fontWeight: 600, color: '#475569' } }, data.difficulty),
          ])
        ]);
      };

      // Custom Tooltip for Bar Chart (Study Activity Volume)
      const CustomBarTooltip = function(props) {
        if (!props || !props.active || !props.payload || !props.payload.length) return null;
        const data = props.payload[0]?.payload;
        if (!data) return null;

        return h('div', {
          style: {
            backgroundColor: '#ffffff',
            borderRadius: '10px',
            padding: '12px 14px',
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.14), 0 8px 10px -6px rgba(0, 0, 0, 0.08)',
            border: '1px solid #e2e8f0',
            minWidth: '230px',
            maxWidth: '300px',
            fontSize: '12px',
            fontFamily: 'inherit',
            color: '#1e293b',
            pointerEvents: 'none',
          }
        }, [
          // Header: Category badge & volume total
          h('div', {
            key: 'hdr',
            style: {
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '8px',
              borderBottom: '1px solid #f1f5f9',
              paddingBottom: '6px',
            }
          }, [
            h('span', {
              key: 'type-badge',
              style: {
                backgroundColor: `${data.fill}18`,
                color: data.fill,
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '4px',
                fontSize: '10.5px',
                textTransform: 'uppercase',
                letterSpacing: '0.4px',
              }
            }, data.activityType),
            h('span', {
              key: 'total-badge',
              style: {
                fontWeight: 700,
                color: data.fill,
                fontSize: '12px',
              }
            }, `${data.count} Completed`)
          ]),

          // Category Title
          h('div', {
            key: 'category-title',
            style: {
              fontWeight: 700,
              fontSize: '13px',
              color: '#0f172a',
              marginBottom: '8px',
              lineHeight: '1.3',
            }
          }, data.label),

          // Structured Data Grid
          h('div', {
            key: 'details-grid',
            style: {
              display: 'grid',
              gridTemplateColumns: 'auto 1fr',
              gap: '5px 12px',
              fontSize: '11.5px',
              lineHeight: '1.4',
            }
          }, [
            h('span', { key: 'l-act', style: { color: '#64748b', fontWeight: 500 } }, 'Activity Type:'),
            h('span', { key: 'v-act', style: { fontWeight: 600, color: '#1e293b' } }, data.activityType),

            h('span', { key: 'l-score', style: { color: '#64748b', fontWeight: 500 } }, 'Score:'),
            h('span', { key: 'v-score', style: { fontWeight: 700, color: '#4f46e5' } }, data.scoreMetric),

            h('span', { key: 'l-date', style: { color: '#64748b', fontWeight: 500 } }, 'Precise Date:'),
            h('span', { key: 'v-date', style: { fontWeight: 600, color: '#334155' } }, data.preciseDate),
          ])
        ]);
      };

      // Render Line Chart
      if (lineData.length === 0) {
        lineChartContainer.innerHTML = `
          <div class="d-flex flex-column align-items-center justify-content-center text-center p-3 text-muted" style="height: 250px;">
            <div class="rounded-circle d-flex align-items-center justify-content-center mb-2" style="width: 44px; height: 44px; background: #eef2ff; color: #4f46e5;">
              <i class="bi bi-graph-up fs-5"></i>
            </div>
            <div class="fw-semibold text-dark mb-1" style="font-size: 0.85rem;">No Quiz Scores Logged Yet</div>
            <p class="text-muted mb-2" style="font-size: 0.75rem; max-width: 280px;">Complete an AI-generated multiple choice quiz to start tracking your daily score trends.</p>
            <button class="btn btn-sm btn-indigo px-3 py-1" onclick="window.appUI.switchView('quiz')" style="font-size: 0.75rem;">
              <i class="bi bi-patch-question me-1"></i> Start Practice Quiz
            </button>
          </div>
        `;
        if (this.lineChartRoot) {
          try { this.lineChartRoot.unmount(); } catch (e) {}
          this.lineChartRoot = null;
        }
      } else {
        lineChartContainer.innerHTML = '';
        const lineChartNode = h(
          ResponsiveContainer,
          { width: '100%', height: 250 },
          h(
            LineChart,
            { data: lineData, margin: { top: 15, right: 25, left: -10, bottom: 5 } },
            h(CartesianGrid, { strokeDasharray: '3 3', stroke: '#f1f5f9' }),
            h(XAxis, { dataKey: 'date', tick: { fontSize: 11, fill: '#64748b' }, stroke: '#cbd5e1' }),
            h(YAxis, { domain: [0, 100], unit: '%', tick: { fontSize: 11, fill: '#64748b' }, stroke: '#cbd5e1' }),
            h(Tooltip, {
              content: CustomLineTooltip,
              cursor: { stroke: '#6366f1', strokeWidth: 1.5, strokeDasharray: '4 4' },
            }),
            h(ReferenceLine, { y: 70, stroke: '#10b981', strokeDasharray: '4 4', label: { value: 'Target (70%)', fill: '#059669', fontSize: 10, position: 'insideTopRight' } }),
            h(Line, {
              type: 'monotone',
              dataKey: 'score',
              name: 'Score',
              stroke: '#4f46e5',
              strokeWidth: 3,
              activeDot: { r: 6, fill: '#4f46e5' },
              dot: { r: 4, fill: '#6366f1', strokeWidth: 2, stroke: '#ffffff' }
            })
          )
        );

        if (!this.lineChartRoot) {
          // @ts-ignore
          this.lineChartRoot = window.ReactDOM.createRoot(lineChartContainer);
        }
        this.lineChartRoot.render(lineChartNode);
      }

      // Render Bar Chart
      barChartContainer.innerHTML = '';
      const barChartNode = h(
        ResponsiveContainer,
        { width: '100%', height: 250 },
        h(
          BarChart,
          { data: barData, margin: { top: 15, right: 15, left: -10, bottom: 5 } },
          h(CartesianGrid, { strokeDasharray: '3 3', stroke: '#f1f5f9' }),
          h(XAxis, { dataKey: 'activity', tick: { fontSize: 11, fill: '#64748b' }, stroke: '#cbd5e1' }),
          h(YAxis, { allowDecimals: false, tick: { fontSize: 11, fill: '#64748b' }, stroke: '#cbd5e1' }),
          h(Tooltip, {
            content: CustomBarTooltip,
            cursor: { fill: 'rgba(99, 102, 241, 0.08)' },
          }),
          h(
            Bar,
            { dataKey: 'count', radius: [6, 6, 0, 0], barSize: 44 },
            barData.map((entry, index) => h(Cell, { key: `bar-cell-${index}`, fill: entry.fill }))
          )
        )
      );

      if (!this.barChartRoot) {
        // @ts-ignore
        this.barChartRoot = window.ReactDOM.createRoot(barChartContainer);
      }
      this.barChartRoot.render(barChartNode);
    }
  }

  escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // --- Printable PDF Performance Report Generator ---
  async openPdfReportModal() {
    const reportModalEl = document.getElementById('pdfReportModal');
    if (!reportModalEl) return;

    this.showToast('Preparing academic performance report...', 'info');

    try {
      // 1. Fetch latest verified stats and learning history from backend via AJAX
      const [stats, history] = await Promise.all([
        window.apiService.getDashboardStats().catch(() => ({})),
        window.apiService.getHistory().catch(() => ({ summaries: [], quizzes: [], doubts: [] }))
      ]);

      const user = window.apiService.getCurrentUser() || { name: 'Student', email: 'student@aistudybuddy.com' };
      const rawAvg = stats?.averageQuizScore ?? stats?.average_quiz_score ?? 0;
      const avgScore = (typeof rawAvg === 'number' && Number.isFinite(rawAvg)) ? Math.max(0, Math.min(100, Math.round(rawAvg))) : 0;
      const summariesCount = stats?.totalSummaries ?? stats?.total_summaries ?? (history.summaries?.length || 0);
      const quizzesCount = stats?.totalQuizzes ?? stats?.total_quizzes ?? (history.quizzes?.filter(q => q.score !== null)?.length || 0);

      // 2. Populate Student & Header Metadata
      const studentNameEl = document.getElementById('reportStudentName');
      const studentEmailEl = document.getElementById('reportStudentEmail');
      const issueDateEl = document.getElementById('reportIssueDate');
      const refIdEl = document.getElementById('reportRefId');
      const backendStatusEl = document.getElementById('reportBackendStatus');
      const sysHashEl = document.getElementById('reportSystemHash');

      if (studentNameEl) studentNameEl.textContent = user.name || 'Alex Johnson';
      if (studentEmailEl) studentEmailEl.textContent = user.email || 'alex@aistudybuddy.com';
      if (issueDateEl) {
        issueDateEl.textContent = new Date().toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'long',
          day: 'numeric'
        });
      }
      if (refIdEl) {
        const randId = Math.floor(1000 + Math.random() * 9000);
        refIdEl.textContent = `ASB-REP-${randId}`;
      }
      if (backendStatusEl) {
        backendStatusEl.textContent = window.apiService.isLive()
          ? 'MySQL 8.0 & Spring Boot (Live Synced)'
          : 'Demo Client Engine (Preview)';
      }
      if (sysHashEl) {
        const fakeHash = Array.from({ length: 8 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
        sysHashEl.textContent = `SHA256:${fakeHash}c4...9a2e`;
      }

      // 3. Populate Core Performance Indicators (Summaries, Quizzes taken, Avg Score)
      document.getElementById('reportTotalSummaries').textContent = summariesCount;
      document.getElementById('reportTotalQuizzes').textContent = quizzesCount;
      document.getElementById('reportAvgScore').textContent = `${avgScore}%`;

      // 4. Mastery Grade & Progress Bar
      let grade = 'N/A';
      let diagnostic = 'No quizzes taken yet. Complete a practice quiz to generate performance analytics.';
      let progressColor = 'bg-secondary';

      if (quizzesCount > 0) {
        if (avgScore >= 90) {
          grade = 'Mastery: Grade A+ (Exceptional)';
          diagnostic = 'Outstanding performance! Student exhibits exceptional mastery of study concepts.';
          progressColor = 'bg-success';
        } else if (avgScore >= 80) {
          grade = 'Mastery: Grade A (Excellent)';
          diagnostic = 'High level of subject comprehension with strong problem-solving accuracy.';
          progressColor = 'bg-success';
        } else if (avgScore >= 70) {
          grade = 'Mastery: Grade B (Good)';
          diagnostic = 'Good foundational grasp of study material with consistent comprehension.';
          progressColor = 'bg-primary';
        } else if (avgScore >= 60) {
          grade = 'Mastery: Grade C (Satisfactory)';
          diagnostic = 'Satisfactory performance. Recommend focused review of lecture notes summaries.';
          progressColor = 'bg-warning';
        } else {
          grade = 'Mastery: Progressing';
          diagnostic = 'Additional study recommended. Review AI-generated notes summaries and retake quizzes.';
          progressColor = 'bg-danger';
        }
      }

      const gradeBadge = document.getElementById('reportGradeBadge');
      if (gradeBadge) gradeBadge.textContent = grade;

      const scoreBarLabel = document.getElementById('reportScoreBarLabel');
      if (scoreBarLabel) scoreBarLabel.textContent = `${avgScore}%`;

      const scoreProgressBar = document.getElementById('reportScoreProgressBar');
      if (scoreProgressBar) {
        scoreProgressBar.style.width = `${avgScore}%`;
        scoreProgressBar.className = `progress-bar ${progressColor}`;
      }

      const diagEl = document.getElementById('reportDiagnosticText');
      if (diagEl) diagEl.textContent = diagnostic;

      // 5. Populate Detailed Quizzes Taken Log
      const quizzesTbody = document.getElementById('reportQuizzesTableBody');
      const quizCountBadge = document.getElementById('reportQuizCountBadge');
      const quizzesList = (history.quizzes || []).filter(q => q.score !== null);

      if (quizCountBadge) quizCountBadge.textContent = `${quizzesList.length} test${quizzesList.length === 1 ? '' : 's'}`;

      if (quizzesTbody) {
        if (quizzesList.length === 0) {
          quizzesTbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-2.5">No graded quizzes taken yet. Generate and complete a quiz to populate records.</td></tr>`;
        } else {
          quizzesTbody.innerHTML = quizzesList.map((q, idx) => {
            const totalQ = q.totalQuestions || q.total_questions || 5;
            const score = q.score ?? 0;
            const pct = Math.round((score / totalQ) * 100);
            const dateStr = q.completedAt || q.completed_at || q.createdAt || q.created_at;
            const formattedDate = dateStr ? new Date(dateStr).toLocaleDateString() : 'Recent';
            const badgeClass = pct >= 80 ? 'bg-success-subtle text-success border-success-subtle' : pct >= 60 ? 'bg-primary-subtle text-primary border-primary-subtle' : 'bg-warning-subtle text-warning border-warning-subtle';

            return `
              <tr>
                <td class="text-muted font-mono-nums">${idx + 1}</td>
                <td class="fw-semibold text-dark text-truncate" style="max-width: 250px;">${this.escapeHtml(q.topic || 'General Quiz')}</td>
                <td><span class="badge bg-light text-dark border px-1.5">${this.escapeHtml(q.difficulty || 'Medium')}</span></td>
                <td class="text-center font-mono-nums fw-bold">${score} / ${totalQ}</td>
                <td class="text-center"><span class="badge ${badgeClass} border font-mono-nums">${pct}%</span></td>
                <td class="text-muted" style="font-size: 0.72rem;">${formattedDate}</td>
              </tr>
            `;
          }).join('');
        }
      }

      // 6. Populate Notes Summaries Inventory
      const notesTbody = document.getElementById('reportNotesTableBody');
      const notesCountBadge = document.getElementById('reportNotesCountBadge');
      const notesList = history.summaries || [];

      if (notesCountBadge) notesCountBadge.textContent = `${notesList.length} summar${notesList.length === 1 ? 'y' : 'ies'}`;

      if (notesTbody) {
        if (notesList.length === 0) {
          notesTbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-2.5">No study notes summarized yet.</td></tr>`;
        } else {
          notesTbody.innerHTML = notesList.slice(0, 8).map((n, idx) => {
            const dateStr = n.createdAt || n.created_at;
            const formattedDate = dateStr ? new Date(dateStr).toLocaleDateString() : 'Recent';
            return `
              <tr>
                <td class="text-muted font-mono-nums">${idx + 1}</td>
                <td class="fw-semibold text-dark text-truncate" style="max-width: 320px;">${this.escapeHtml(n.title || 'Study Summary')}</td>
                <td class="text-muted" style="font-size: 0.72rem;">${formattedDate}</td>
                <td class="text-center"><span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 0.65rem;">Gemini AI Synthesized</span></td>
              </tr>
            `;
          }).join('');
        }
      }

      // 7. Show the modal
      const modalInstance = bootstrap.Modal.getOrCreateInstance(reportModalEl);
      modalInstance.show();
    } catch (err) {
      console.error('Error opening PDF report modal:', err);
      this.showToast('Failed to prepare performance report: ' + err.message, 'error');
    }
  }

  downloadPdfReport() {
    const reportElement = document.getElementById('printablePerformanceReport');
    if (!reportElement) return;

    const user = window.apiService.getCurrentUser() || { name: 'Student' };
    const sanitizedName = (user.name || 'Student').replace(/[^a-zA-Z0-9]/g, '_');
    const filename = `AI_Study_Buddy_Performance_Report_${sanitizedName}.pdf`;

    this.showToast('Generating PDF file directly in your browser...', 'info');

    // Use html2pdf bundle if available
    // @ts-ignore
    if (typeof html2pdf !== 'undefined') {
      const opt = {
        margin: [8, 8, 8, 8],
        filename: filename,
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: { scale: 2, useCORS: true, logging: false },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' }
      };

      // @ts-ignore
      html2pdf().set(opt).from(reportElement).save().then(() => {
        this.showToast('PDF performance report downloaded successfully!', 'success');
      }).catch((err) => {
        console.warn('html2pdf generation encountered an issue, falling back to browser print:', err);
        window.print();
      });
    } else {
      // Direct browser printable fallback
      window.print();
    }
  }

  printPdfReport() {
    // Native browser print dialog allows printing or saving as PDF
    window.print();
  }

  renderRecentActivities(history) {
    const list = document.getElementById('recentActivitiesList');
    if (!list) return;

    const items = [];
    (history.summaries || []).slice(0, 3).forEach((n) => {
      items.push({
        type: 'summary',
        title: n.title,
        subtitle: 'Summarized Notes',
        date: new Date(n.created_at).toLocaleDateString(),
        icon: 'bi-file-text text-primary',
      });
    });

    (history.quizzes || []).slice(0, 3).forEach((q) => {
      items.push({
        type: 'quiz',
        title: q.topic,
        subtitle: q.score !== null ? `Score: ${q.score}/${q.total_questions}` : 'Not graded',
        date: new Date(q.created_at).toLocaleDateString(),
        icon: 'bi-patch-question text-success',
      });
    });

    if (items.length === 0) {
      list.innerHTML = `<div class="text-center py-3 text-muted" style="font-size: 0.8rem;">No recent activities found. Start by summarizing notes or generating a quiz!</div>`;
      return;
    }

    list.innerHTML = items
      .map(
        (it) => `
      <div class="d-flex align-items-center justify-content-between py-2 border-bottom">
        <div class="d-flex align-items-center gap-2.5 text-truncate me-2">
          <i class="bi ${it.icon} fs-5"></i>
          <div class="text-truncate">
            <div class="fw-bold text-dark text-truncate" style="font-size: 0.825rem;">${it.title}</div>
            <small class="text-muted" style="font-size: 0.72rem;">${it.subtitle}</small>
          </div>
        </div>
        <small class="text-muted text-nowrap font-mono-nums" style="font-size: 0.72rem;">${it.date}</small>
      </div>
    `
      )
      .join('');
  }

  // --- Notes Summarizer ---
  loadSampleNotes(sampleKey) {
    const sample = SAMPLES[sampleKey];
    if (sample) {
      document.getElementById('noteTitleInput').value = sample.title;
      const contentEl = document.getElementById('noteContentInput');
      contentEl.value = sample.content;
      document.getElementById('notesCharCount').textContent = `${sample.content.length} characters`;
      this.showToast(`Loaded sample notes: ${sample.title.split(':')[0]}`, 'info');
    }
  }

  async handleNotesSummarize(e) {
    e.preventDefault();
    const title = document.getElementById('noteTitleInput').value.trim();
    const content = document.getElementById('noteContentInput').value.trim();

    if (!content || content.length < 15) {
      this.showToast('Please enter at least a few sentences of study notes.', 'error');
      return;
    }

    const btn = document.getElementById('summarizeBtn');
    const origText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1" role="status"></span> Summarizing with Gemini...`;

    try {
      const res = await window.apiService.summarizeNotes(title, content);
      document.getElementById('summaryPlaceholder').classList.add('d-none');
      document.getElementById('summaryResult').classList.remove('d-none');

      document.getElementById('summaryResultTitle').textContent = res.note.title;
      document.getElementById('summaryResultBody').textContent = res.note.summary;

      this.showToast('Study notes summarized and saved to MySQL!', 'success');
      this.refreshDashboard();
    } catch (err) {
      this.showToast(err.message || 'Summarization failed.', 'error');
    } finally {
      btn.disabled = false;
      btn.innerHTML = origText;
    }
  }

  copySummaryContent() {
    const text = document.getElementById('summaryResultBody').textContent;
    navigator.clipboard.writeText(text);
    const copyTextEl = document.getElementById('copySummaryBtnText');
    copyTextEl.textContent = 'Copied!';
    this.showToast('Summary copied to clipboard!', 'success');
    setTimeout(() => {
      copyTextEl.textContent = 'Copy';
    }, 2000);
  }

  // --- Quiz Generator & Authoritative Grading ---
  async handleGenerateQuiz(e) {
    e.preventDefault();
    const topic = document.getElementById('quizTopicInput').value.trim();
    const count = parseInt(document.querySelector('input[name="quizCountRadio"]:checked').value);
    const difficulty = document.querySelector('input[name="quizDiffRadio"]:checked').value;

    if (!topic) {
      this.showToast('Please specify a quiz topic.', 'error');
      return;
    }

    const btn = document.getElementById('generateQuizBtn');
    const origText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Formulating MCQs...`;

    try {
      const quiz = await window.apiService.generateQuiz(topic, count, difficulty);
      this.activeQuiz = quiz;
      this.renderActiveQuiz();
      document.getElementById('quizConfigCard').classList.add('d-none');
      document.getElementById('quizActiveContainer').classList.remove('d-none');
      document.getElementById('resetQuizBtn').classList.remove('d-none');
      this.showToast(`Generated ${quiz.questions.length} questions on ${quiz.topic}!`, 'success');
    } catch (err) {
      this.showToast(err.message || 'Quiz generation failed.', 'error');
    } finally {
      btn.disabled = false;
      btn.innerHTML = origText;
    }
  }

  renderActiveQuiz(gradedResult = null) {
    const container = document.getElementById('quizQuestionsList');
    if (!this.activeQuiz || !container) return;

    const questions = gradedResult ? gradedResult.questions : this.activeQuiz.questions;
    const submittedAnswers = gradedResult ? gradedResult.submitted_answers : {};

    container.innerHTML = questions
      .map((q, idx) => {
        const letters = ['A', 'B', 'C', 'D'];
        const chosen = submittedAnswers[idx];

        return `
        <div class="card card-modern p-4 mb-3" id="questionCard-${idx}">
          <div class="d-flex align-items-center justify-content-between mb-2">
            <span class="badge bg-light text-primary border" style="font-size: 0.72rem;">Question ${idx + 1} of ${questions.length}</span>
          </div>
          <h6 class="fw-bold text-dark mb-3" style="line-height: 1.5;">${q.question}</h6>

          <div class="d-flex flex-column gap-2">
            ${q.options
              .map((opt, optIdx) => {
                const optLetter = letters[optIdx];
                const inputName = `q_answer_${idx}`;
                const inputId = `opt_${idx}_${optIdx}`;
                const isChecked = chosen === opt ? 'checked' : '';
                const isDisabled = gradedResult ? 'disabled' : '';

                let highlightClass = '';
                if (gradedResult) {
                  if (opt.trim().toLowerCase() === q.correctAnswer?.trim().toLowerCase()) {
                    highlightClass = 'border-success bg-success-subtle text-success fw-bold';
                  } else if (chosen === opt && chosen !== q.correctAnswer) {
                    highlightClass = 'border-danger bg-danger-subtle text-danger';
                  }
                }

                return `
                <div class="form-check p-0">
                  <input type="radio" class="btn-check" name="${inputName}" id="${inputId}" value="${opt.replace(/"/g, '&quot;')}" ${isChecked} ${isDisabled} required />
                  <label class="btn btn-outline-secondary w-100 text-start d-flex align-items-center gap-2.5 py-2.5 px-3 rounded-3 ${highlightClass}" for="${inputId}" style="font-size: 0.85rem;">
                    <span class="badge bg-white text-dark border fw-bold" style="font-size: 0.7rem;">${optLetter}</span>
                    <span>${opt}</span>
                  </label>
                </div>
              `;
              })
              .join('')}
          </div>

          ${
            gradedResult && q.explanation
              ? `
            <div class="mt-3 p-3 bg-light rounded-3 border" style="font-size: 0.8rem; line-height: 1.5;">
              <span class="fw-bold text-dark d-block mb-1"><i class="bi bi-info-circle-fill text-primary me-1"></i>Explanation:</span>
              <span class="text-muted">${q.explanation}</span>
            </div>
          `
              : ''
          }
        </div>
      `;
      })
      .join('');
  }

  async handleSubmitQuiz(e) {
    e.preventDefault();
    if (!this.activeQuiz) return;

    const answers = {};
    const unanswered = [];
    this.activeQuiz.questions.forEach((_, idx) => {
      const selected = document.querySelector(`input[name="q_answer_${idx}"]:checked`);
      if (selected && selected.value.trim()) {
        answers[idx] = selected.value.trim();
      } else {
        unanswered.push(idx + 1);
      }
    });

    if (unanswered.length > 0) {
      this.showToast(`Please answer every question before submitting. Unanswered: #${unanswered.join(', #')}`, 'warning');
      return;
    }

    const submitBtn = document.getElementById('submitQuizBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Grading Answers...`;

    try {
      const result = await window.apiService.submitQuiz(this.activeQuiz.quiz_id, answers);

      // Render score banner
      const banner = document.getElementById('quizScoreBanner');
      banner.classList.remove('d-none');
      document.getElementById('scoreBannerPercentage').textContent = `${result.percentage}%`;
      document.getElementById('scoreBannerFraction').textContent = `Score: ${result.score} / ${result.total_questions}`;

      if (result.percentage >= 60) {
        document.getElementById('scoreBannerHeading').textContent = 'Well Done! You Passed!';
      } else {
        document.getElementById('scoreBannerHeading').textContent = 'Review Recommended';
      }

      // Hide submit button footer and re-render with explanations
      document.getElementById('quizSubmitFooter').classList.add('d-none');
      this.renderActiveQuiz(result);

      this.showToast(`Quiz graded! Your score: ${result.score}/${result.total_questions} (${result.percentage}%)`, 'success');
      this.refreshDashboard();
    } catch (err) {
      this.showToast(err.message || 'Quiz submission failed.', 'error');
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = `<i class="bi bi-check2-circle me-1"></i> Submit Quiz for Grading`;
    }
  }

  resetQuizState() {
    this.activeQuiz = null;
    document.getElementById('quizConfigCard').classList.remove('d-none');
    document.getElementById('quizActiveContainer').classList.add('d-none');
    document.getElementById('resetQuizBtn').classList.add('d-none');
    document.getElementById('quizScoreBanner').classList.add('d-none');
    document.getElementById('quizSubmitFooter').classList.remove('d-none');
  }

  // --- Doubt Solver ---
  async renderChatMessages() {
    const history = await window.apiService.getHistory();
    const container = document.getElementById('chatMessages');
    if (!container) return;

    const doubts = history.doubts || [];
    if (doubts.length === 0) {
      container.innerHTML = `
        <div class="d-flex align-items-start gap-2.5">
          <div class="rounded-3 bg-primary text-white p-2 d-flex align-items-center justify-center shrink-0" style="width: 32px; height: 32px;">
            <i class="bi bi-robot"></i>
          </div>
          <div class="p-3 bg-white border rounded-3 text-dark shadow-2xs" style="max-width: 80%; font-size: 0.85rem; line-height: 1.6;">
            <strong>Hello! I am your AI Study Buddy tutor.</strong><br>
            What concept, formula, or academic problem can I help break down for you today? Ask any question!
          </div>
        </div>
      `;
      return;
    }

    container.innerHTML = doubts
      .map(
        (d) => `
      <div class="d-flex flex-column gap-2 mb-2">
        <div class="d-flex align-items-start gap-2.5 justify-content-end">
          <div class="p-3 rounded-3 text-white shadow-2xs" style="background-color: var(--primary-indigo); max-width: 80%; font-size: 0.85rem;">
            ${d.question}
          </div>
          <div class="rounded-3 bg-secondary text-white p-2 d-flex align-items-center justify-center shrink-0" style="width: 32px; height: 32px;">
            <i class="bi bi-person-fill"></i>
          </div>
        </div>

        <div class="d-flex align-items-start gap-2.5">
          <div class="rounded-3 bg-primary text-white p-2 d-flex align-items-center justify-center shrink-0" style="width: 32px; height: 32px; background: linear-gradient(135deg, #4f46e5, #7c3aed) !important;">
            <i class="bi bi-robot"></i>
          </div>
          <div class="p-3 bg-white border rounded-3 text-dark shadow-2xs" style="max-width: 80%; font-size: 0.85rem; line-height: 1.6; white-space: pre-wrap;">
            ${d.answer}
          </div>
        </div>
      </div>
    `
      )
      .join('');

    const scrollContainer = document.getElementById('chatContainer');
    if (scrollContainer) scrollContainer.scrollTop = scrollContainer.scrollHeight;
  }

  async handleSendDoubt(e) {
    if (e) e.preventDefault();
    const input = document.getElementById('doubtInput');
    const question = input.value.trim();
    if (!question) return;

    input.value = '';
    const container = document.getElementById('chatMessages');

    // Append user question
    container.innerHTML += `
      <div class="d-flex align-items-start gap-2.5 justify-content-end mb-2">
        <div class="p-3 rounded-3 text-white shadow-2xs" style="background-color: var(--primary-indigo); max-width: 80%; font-size: 0.85rem;">
          ${question}
        </div>
        <div class="rounded-3 bg-secondary text-white p-2 d-flex align-items-center justify-center shrink-0" style="width: 32px; height: 32px;">
          <i class="bi bi-person-fill"></i>
        </div>
      </div>
    `;

    // Append loading placeholder
    const loadingId = 'loading-' + Date.now();
    container.innerHTML += `
      <div id="${loadingId}" class="d-flex align-items-start gap-2.5 mb-2">
        <div class="rounded-3 bg-primary text-white p-2 d-flex align-items-center justify-center shrink-0" style="width: 32px; height: 32px;">
          <i class="bi bi-robot"></i>
        </div>
        <div class="p-3 bg-white border rounded-3 text-muted" style="font-size: 0.85rem;">
          <span class="spinner-border spinner-border-sm me-1" role="status"></span> Formulating clear explanation...
        </div>
      </div>
    `;

    const scrollContainer = document.getElementById('chatContainer');
    if (scrollContainer) scrollContainer.scrollTop = scrollContainer.scrollHeight;

    try {
      const res = await window.apiService.askDoubt(question);
      const loadingEl = document.getElementById(loadingId);
      if (loadingEl) loadingEl.remove();

      container.innerHTML += `
        <div class="d-flex align-items-start gap-2.5 mb-2">
          <div class="rounded-3 bg-primary text-white p-2 d-flex align-items-center justify-center shrink-0" style="width: 32px; height: 32px; background: linear-gradient(135deg, #4f46e5, #7c3aed) !important;">
            <i class="bi bi-robot"></i>
          </div>
          <div class="p-3 bg-white border rounded-3 text-dark shadow-2xs" style="max-width: 80%; font-size: 0.85rem; line-height: 1.6; white-space: pre-wrap;">
            ${res.doubt.answer}
          </div>
        </div>
      `;

      if (scrollContainer) scrollContainer.scrollTop = scrollContainer.scrollHeight;
    } catch (err) {
      this.showToast(err.message || 'Failed to answer doubt.', 'error');
    }
  }

  async clearChatHistory() {
    if (!confirm('Are you sure you want to clear your AI Tutor doubt conversations?')) return;
    try {
      await window.apiService.clearDoubts();
      await this.renderChatMessages();
      this.showToast('Chat history cleared.', 'info');
      this.refreshDashboard();
    } catch (e) {
      this.showToast(e.message || 'Failed to clear chat.', 'error');
    }
  }

  // --- Learning History ---
  async renderHistoryList() {
    const container = document.getElementById('historyItemsContainer');
    if (!container) return;

    const history = await window.apiService.getHistory();
    const activeFilter = document.querySelector('.history-filter-btn.active')?.dataset.filter || 'all';
    const search = document.getElementById('historySearchInput').value.toLowerCase().trim();

    const summaries = (history.summaries || []).filter(
      (n) =>
        (activeFilter === 'all' || activeFilter === 'summaries') &&
        (n.title.toLowerCase().includes(search) || n.summary.toLowerCase().includes(search))
    );

    const quizzes = (history.quizzes || []).filter(
      (q) =>
        (activeFilter === 'all' || activeFilter === 'quizzes') &&
        q.topic.toLowerCase().includes(search)
    );

    const doubts = (history.doubts || []).filter(
      (d) =>
        (activeFilter === 'all' || activeFilter === 'doubts') &&
        (d.question.toLowerCase().includes(search) || d.answer.toLowerCase().includes(search))
    );

    if (summaries.length === 0 && quizzes.length === 0 && doubts.length === 0) {
      container.innerHTML = `
        <div class="col-12 text-center py-5">
          <p class="text-muted fw-semibold">No study records match your filter criteria.</p>
        </div>
      `;
      return;
    }

    let html = '';

    summaries.forEach((note) => {
      const noteId = note.noteId || note.note_id;
      const createdAt = note.createdAt || note.created_at;
      html += `
        <div class="col-12 col-md-6">
          <div class="card card-modern p-4 h-100 d-flex flex-column justify-content-between">
            <div>
              <div class="d-flex align-items-center justify-content-between mb-2">
                <span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 0.7rem;">Summary</span>
                <button class="btn btn-sm text-danger p-0" onclick="window.appUI.deleteNoteItem(${noteId})" title="Delete">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
              <h6 class="fw-bold text-dark mb-2 text-truncate">${note.title}</h6>
              <p class="text-muted" style="font-size: 0.8rem; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;">
                ${note.summary.replace(/[#*`]/g, '')}
              </p>
            </div>
            <div class="pt-3 border-top d-flex align-items-center justify-content-between">
              <small class="text-muted font-mono-nums" style="font-size: 0.72rem;">${createdAt ? new Date(createdAt).toLocaleDateString() : 'Recent'}</small>
              <button class="btn btn-sm btn-link text-primary p-0 text-decoration-none fw-semibold" onclick="window.appUI.openNoteModal(${noteId})" style="font-size: 0.75rem;">
                Read full summary &rarr;
              </button>
            </div>
          </div>
        </div>
      `;
    });

    quizzes.forEach((quiz) => {
      const quizId = quiz.quizId || quiz.quiz_id;
      const totalQuestions = quiz.totalQuestions || quiz.total_questions || 5;
      const isGraded = quiz.score !== null && quiz.score !== undefined;
      const pct = isGraded ? Math.round((quiz.score / totalQuestions) * 100) : null;
      const createdAt = quiz.createdAt || quiz.created_at;

      html += `
        <div class="col-12 col-md-6">
          <div class="card card-modern p-4 h-100 d-flex flex-column justify-content-between">
            <div>
              <div class="d-flex align-items-center justify-content-between mb-2">
                <span class="badge bg-success-subtle text-success border border-success-subtle" style="font-size: 0.7rem;">Quiz · ${quiz.difficulty}</span>
                <button class="btn btn-sm text-danger p-0" onclick="window.appUI.deleteQuizItem(${quizId})" title="Delete">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
              <h6 class="fw-bold text-dark mb-2 text-truncate">${quiz.topic}</h6>
              <div class="mb-3">
                ${
                  isGraded
                    ? `<span class="badge ${pct >= 60 ? 'bg-success' : 'bg-warning text-dark'} font-mono-nums" style="font-size: 0.75rem;">Score: ${quiz.score}/${totalQuestions} (${pct}%)</span>`
                    : `<span class="badge bg-light text-muted border">In Progress</span>`
                }
              </div>
            </div>
            <div class="pt-3 border-top d-flex align-items-center justify-content-between">
              <small class="text-muted font-mono-nums" style="font-size: 0.72rem;">${createdAt ? new Date(createdAt).toLocaleDateString() : 'Recent'}</small>
              <button class="btn btn-sm btn-link text-success p-0 text-decoration-none fw-semibold" onclick="window.appUI.openQuizModal(${quizId})" style="font-size: 0.75rem;">
                Review questions &rarr;
              </button>
            </div>
          </div>
        </div>
      `;
    });

    doubts.forEach((doubt) => {
      const doubtId = doubt.doubtId || doubt.doubt_id;
      const createdAt = doubt.createdAt || doubt.created_at;
      html += `
        <div class="col-12 col-md-6">
          <div class="card card-modern p-4 h-100 d-flex flex-column justify-content-between">
            <div>
              <div class="d-flex align-items-center justify-content-between mb-2">
                <span class="badge border" style="font-size: 0.7rem; background-color: #f3e8ff; color: #7e22ce; border-color: #ddd6fe !important;">Doubt Session</span>
                <button class="btn btn-sm text-danger p-0" onclick="window.appUI.deleteDoubtItem(${doubtId})" title="Delete">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
              <h6 class="fw-bold text-dark mb-2 text-truncate">${doubt.question}</h6>
              <p class="text-muted" style="font-size: 0.8rem; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;">
                ${doubt.answer.replace(/[#*`]/g, '')}
              </p>
            </div>
            <div class="pt-3 border-top d-flex align-items-center justify-content-between">
              <small class="text-muted font-mono-nums" style="font-size: 0.72rem;">${createdAt ? new Date(createdAt).toLocaleDateString() : 'Recent'}</small>
              <button class="btn btn-sm btn-link p-0 text-decoration-none fw-semibold" onclick="window.appUI.openDoubtModal(${doubtId})" style="font-size: 0.75rem; color: #7e22ce;">
                View explanation &rarr;
              </button>
            </div>
          </div>
        </div>
      `;
    });

    container.innerHTML = html;
  }

  async openNoteModal(id) {
    const history = await window.apiService.getHistory();
    const note = (history.summaries || []).find((n) => (n.noteId || n.note_id) === id);
    if (!note) return;

    document.getElementById('modalCategoryBadge').textContent = 'Study Note Summary';
    document.getElementById('historyModalTitle').textContent = note.title;
    document.getElementById('historyModalBody').textContent = note.summary;

    document.getElementById('modalCopyBtn').onclick = () => {
      navigator.clipboard.writeText(note.summary);
      this.showToast('Summary copied to clipboard!', 'success');
    };

    new bootstrap.Modal(document.getElementById('historyDetailModal')).show();
  }

  async openQuizModal(id) {
    const history = await window.apiService.getHistory();
    const quiz = (history.quizzes || []).find((q) => (q.quizId || q.quiz_id) === id);
    if (!quiz) return;

    const totalQuestions = quiz.totalQuestions || quiz.total_questions || 5;
    document.getElementById('modalCategoryBadge').textContent = `Quiz Review (${quiz.difficulty})`;
    document.getElementById('historyModalTitle').textContent = `${quiz.topic} - Score: ${quiz.score ?? 'N/A'}/${totalQuestions}`;

    let bodyText = `Topic: ${quiz.topic}\nDifficulty: ${quiz.difficulty}\nScore: ${quiz.score ?? 'N/A'}/${totalQuestions}\n\nQuestions & Answers:\n\n`;

    const qList = typeof quiz.questions === 'string' ? JSON.parse(quiz.questions) : (quiz.questions || []);

    qList.forEach((q, i) => {
      const opts = Array.isArray(q.options) ? q.options.join(', ') : '';
      bodyText += `${i + 1}. ${q.question}\nOptions: ${opts}\nCorrect Answer: ${q.correctAnswer || 'Evaluated server-side'}\nExplanation: ${q.explanation || 'Verified'}\n\n`;
    });

    document.getElementById('historyModalBody').textContent = bodyText;

    document.getElementById('modalCopyBtn').onclick = () => {
      navigator.clipboard.writeText(bodyText);
      this.showToast('Quiz details copied!', 'success');
    };

    new bootstrap.Modal(document.getElementById('historyDetailModal')).show();
  }

  async openDoubtModal(id) {
    const history = await window.apiService.getHistory();
    const doubt = (history.doubts || []).find((d) => (d.doubtId || d.doubt_id) === id);
    if (!doubt) return;

    document.getElementById('modalCategoryBadge').textContent = 'AI Doubt Explanation';
    document.getElementById('historyModalTitle').textContent = doubt.question;
    document.getElementById('historyModalBody').textContent = doubt.answer;

    document.getElementById('modalCopyBtn').onclick = () => {
      navigator.clipboard.writeText(doubt.answer);
      this.showToast('Explanation copied to clipboard!', 'success');
    };

    new bootstrap.Modal(document.getElementById('historyDetailModal')).show();
  }

  async deleteNoteItem(id) {
    if (!confirm('Are you sure you want to delete this study summary?')) return;
    try {
      await window.apiService.deleteNote(id);
      this.showToast('Study note deleted.', 'success');
      this.renderHistoryList();
      this.refreshDashboard();
    } catch (e) {
      this.showToast(e.message || 'Failed to delete note.', 'error');
    }
  }

  async deleteQuizItem(id) {
    if (!confirm('Are you sure you want to delete this quiz record?')) return;
    try {
      await window.apiService.deleteQuiz(id);
      this.showToast('Quiz record deleted.', 'success');
      this.renderHistoryList();
      this.refreshDashboard();
    } catch (e) {
      this.showToast(e.message || 'Failed to delete quiz.', 'error');
    }
  }

  async deleteDoubtItem(id) {
    if (!confirm('Are you sure you want to delete this doubt session?')) return;
    try {
      await window.apiService.deleteDoubt(id);
      this.showToast('Doubt conversation deleted.', 'success');
      this.renderHistoryList();
      this.refreshDashboard();
    } catch (e) {
      this.showToast(e.message || 'Failed to delete doubt.', 'error');
    }
  }

  // --- Java Spring Boot Hub ---
  renderJavaFilesList() {
    const list = document.getElementById('javaFileList');
    if (!list) return;

    list.innerHTML = JAVA_FILES.map(
      (file) => `
      <a href="#" class="list-group-item list-group-item-action d-flex align-items-center justify-content-between p-2.5 rounded-2 mb-1 border-0 ${
        file.name === this.selectedJavaFile.name ? 'active' : ''
      }" data-filename="${file.name}" style="font-size: 0.8rem;">
        <span class="d-flex align-items-center gap-2 text-truncate">
          <i class="bi bi-filetype-java"></i>
          <span class="text-truncate">${file.name}</span>
        </span>
        <span class="badge bg-light text-dark border" style="font-size: 0.65rem;">${file.category}</span>
      </a>
    `
    ).join('');

    list.querySelectorAll('.list-group-item').forEach((item) => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        const fName = item.dataset.filename;
        const file = JAVA_FILES.find((f) => f.name === fName);
        if (file) {
          this.selectedJavaFile = file;
          this.renderJavaFilesList();
          this.renderJavaFileDetail();
        }
      });
    });
  }

  renderJavaFileDetail() {
    const file = this.selectedJavaFile;
    if (!file) return;

    document.getElementById('selectedFileName').textContent = file.name;
    document.getElementById('selectedFilePath').textContent = file.path;
    document.getElementById('selectedFileBadge').textContent = file.category;
    document.getElementById('javaCodeDisplay').textContent = file.code;
  }

  copySelectedJavaCode() {
    navigator.clipboard.writeText(this.selectedJavaFile.code);
    const copyText = document.getElementById('copyJavaCodeBtnText');
    copyText.textContent = 'Copied!';
    this.showToast(`Copied ${this.selectedJavaFile.name} to clipboard!`, 'success');
    setTimeout(() => {
      copyText.textContent = 'Copy Code';
    }, 2000);
  }

  downloadSelectedJavaFile() {
    const blob = new Blob([this.selectedJavaFile.code], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = this.selectedJavaFile.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    this.showToast(`Downloaded ${this.selectedJavaFile.name}`, 'success');
  }

  // --- Auth Handlers ---
  async handleAuthSubmit(e) {
    e.preventDefault();
    const mode = document.querySelector('input[name="authModeRadio"]:checked').value;
    const email = document.getElementById('authEmailInput').value.trim();
    const password = document.getElementById('authPasswordInput').value;
    const name = document.getElementById('authNameInput').value.trim();

    try {
      if (mode === 'register') {
        if (!name) {
          this.showToast('Please enter your full name.', 'error');
          return;
        }
        await window.apiService.register(name, email, password, this.selectedAuthAvatar);
        this.showToast(`Welcome to AI Study Buddy, ${name}!`, 'success');
      } else {
        await window.apiService.login(email, password);
        this.showToast('Welcome back!', 'success');
      }

      this.updateUserUI(window.apiService.getCurrentUser());
      bootstrap.Modal.getInstance(document.getElementById('authModal')).hide();
      this.refreshDashboard();
    } catch (err) {
      this.showToast(err.message || 'Authentication failed.', 'error');
    }
  }

  async handleQuickDemoLogin() {
    try {
      await window.apiService.login('alex@aistudybuddy.com', 'password123');
      this.updateUserUI(window.apiService.getCurrentUser());
      bootstrap.Modal.getInstance(document.getElementById('authModal')).hide();
      this.showToast('Signed in as Demo Student: Alex Johnson!', 'success');
      this.refreshDashboard();
    } catch (err) {
      this.showToast('Demo login failed.', 'error');
    }
  }

  async handleLogout() {
    await window.apiService.logout();
    this.updateUserUI(null);
    this.showToast('Logged out of account. Session token invalidated.', 'info');
    new bootstrap.Modal(document.getElementById('authModal')).show();
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.appUI = new AppUI();
});
