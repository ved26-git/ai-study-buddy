# AI Study Buddy – AI-Powered Learning Platform

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT%20Stateless-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![AI](https://img.shields.io/badge/Gemini%20API-Generative%20AI-purple.svg)](https://aistudio.google.com/)
[![Frontend](https://img.shields.io/badge/Frontend-HTML5%20%7C%20Bootstrap%205%20%7C%20JS-blueviolet.svg)](https://getbootstrap.com/)

A modern, production-quality, full-stack educational web application designed for students and universities. **AI Study Buddy** helps students study faster and retain more by generating structured lecture summaries, multiple-choice quizzes with server-authoritative scoring, and step-by-step doubt resolutions using **Google Gemini Generative AI** orchestrated through a secure **Java 17 Spring Boot** REST backend.

---

## 1. Key Features

* **Secure Authentication & RBAC**:
  * User registration and profile picture upload/preset selector.
  * BCrypt password hashing and HMAC-SHA256 stateless JWT generation.
  * Validated secret strength (minimum 256 bits / 32 bytes required; verified at startup by `EnvironmentValidator`).
  * Server-side logout with persistent `TokenBlacklistService` using SHA-256 token hashing backed by MySQL `revoked_tokens` table. Survives application restarts!
  * Identity extraction strictly through Spring Security's authenticated principal (`SecurityContextHolder`).
  * Row-level MySQL data isolation guaranteeing each student only accesses their own study materials.

* **AI Notes Summarizer**:
  * Ingests lengthy lecture notes and calls Google Gemini API through the Spring Boot backend.
  * Extracts key definitions, exam tips, and bulleted takeaways without meaning distortion.
  * One-click clipboard copying and persistence to MySQL `study_notes` table.

* **AI Quiz Generator with Server-Authoritative Scoring**:
  * Generates 5, 10, or 15 multiple-choice questions across Easy, Medium, or Hard difficulty.
  * **Strict AI Response Validation**: The backend parses and enforces exactly 4 non-empty distinct options per question, guarantees `correctAnswer` matches one of the options, and strictly rejects malformed Gemini responses.
  * **Answer Key Concealment**: Answer keys and explanations are stored strictly in MySQL and concealed from the frontend before submission via `QuizAttemptResponse`.
  * **Duplicate Submission Lock**: Prevents submitting completed quizzes again; returns an explicit error if already graded.
  * **Index & Input Validation**: Validates submitted question indexes (`0 <= index < totalQuestions`) and rejects blank answers.
  * **Server-Side Grading**: The backend compares submitted choices with the stored answer key, calculates the verified score and percentage, and returns `QuizSubmitResponse` with full explanations only upon completion.
  * **Safe List Views**: All quiz history endpoints return `QuizListResponse` (never exposing raw question JSON).

* **AI Academic Doubt Solver**:
  * Real-time student doubt-solving tutor powered by Gemini AI with structured step-by-step reasoning.
  * Persistent chat conversations stored in MySQL `doubts` table with clear chat and per-doubt deletion options.

* **Learning History & Analytics Dashboard**:
  * Real-time dashboard analytics (Total Summaries, Completed Quizzes, Average Score, Total Doubts) calculated from MySQL records.
  * Division-by-zero protection; guarantees 0% when no quizzes are graded, never emitting NaN or Infinity.
  * Dynamic model display: Front-end dynamically queries `GET /api/config` to display the active Gemini model (e.g. `gemini-3.8-flash`).
  * Unified Learning History tab with search and category filtering (All, Summaries, Quizzes, Doubts).
  * **Interactive Recharts Visual Analytics**: Integrated Recharts data visualizations on the student dashboard, including a **Daily Quiz Scores Progression Line Chart** (with score percentages, target reference lines, and interactive tooltips) and a **Study Activity Volume Bar Chart** (comparing lecture note summaries, quizzes taken, and resolved doubts).
  * **In-Browser Printable PDF Performance Report**: One-click generation of an official academic performance transcript featuring student credentials, verified counts for Total Summaries, Quizzes Taken, and Average Score, assessment logs, mastery letter grade, and diagnostic evaluation. Supports direct client-side PDF download (`html2pdf`) or native print-to-PDF (`window.print()`).

* **Dual Mode Architecture**:
  * **Live Mode (Default)**: Directly communicates with Java Spring Boot REST API (`http://localhost:8080/api`) with full MySQL and Gemini AI persistence. Never silently falls back to mock responses on errors.
  * **Demo Mode**: Explicitly selectable for isolated client-side preview in environments without a running Java backend.

---

## 2. Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | HTML5, CSS3, Bootstrap 5.3, JavaScript (ES6+), Bootstrap Icons |
| **Backend** | Java 17, Spring Boot 3.2.4, Spring Web, Spring Security, Spring Data JPA, Hibernate ORM, Maven |
| **Security** | JWT (io.jsonwebtoken 0.11.5), BCryptPasswordEncoder, Persistent Token Revocation, Strict CORS |
| **Database** | MySQL 8.0, InnoDB Storage Engine, Foreign Keys with ON DELETE CASCADE, Composite Indexes |
| **AI Integration** | Google Gemini API (`gemini-3.5-flash`), server-side HTTP calls with markdown fence sanitization |
| **Testing** | JUnit 5, Mockito, MockMvc, H2 In-Memory Test Database |

---

## 3. Project Directory Structure

```text
ai-study-buddy/
├── frontend/                       # Canonical Single-Page Application (HTML5, Bootstrap 5, JS)
│   ├── index.html                  # Main SPA interface
│   ├── css/
│   │   └── style.css               # Clean Bootstrap custom styling & cards
│   ├── js/
│   │   ├── api.js                  # Modular REST API client (Live mode default)
│   │   └── app.js                  # UI controllers & event handlers
│   └── assets/images/              # Student avatar presets
│
├── spring-boot-backend/            # Java 17 + Spring Boot 3.2 Maven Backend
│   ├── pom.xml                     # Maven project configuration & dependencies
│   ├── README.md                   # Complete REST API reference & developer guide
│   ├── postman_collection.json     # Postman collection with JWT automated tokens
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aistudybuddy/
│   │   │   │   ├── config/         # EnvironmentValidator, WebConfig
│   │   │   │   ├── controller/     # Auth, Config, Notes, Quiz, Doubt, Dashboard, History Controllers
│   │   │   │   ├── service/        # AuthService, GeminiService, QuizService, NotesService, etc.
│   │   │   │   ├── repository/     # Spring Data JPA Repositories (incl. RevokedTokenRepository)
│   │   │   │   ├── entity/         # JPA Entities (User, StudyNotes, Quiz, Doubt, RevokedToken)
│   │   │   │   ├── dto/            # Safe transfer objects (QuizAttemptResponse, etc.)
│   │   │   │   ├── security/       # SecurityConfig, JwtTokenProvider, TokenBlacklistService
│   │   │   │   └── exception/      # GlobalExceptionHandler
│   │   │   └── resources/
│   │   │       ├── application.properties # Environment-driven configuration
│   │   │       └── schema.sql      # Production MySQL DDL & Indexes
│   │   └── test/                   # JUnit 5 & MockMvc Automated Tests
│   └── .env.example                # Backend environment template
│
├── postman/                        # Postman test collection
└── .env.example                    # Root environment template
```

---

## 4. Environment Variables

Spring Boot does **not** natively load `.env` files automatically without third-party plugins. Environment variables must be set in your terminal shell or in your IDE Run Configuration.

### Linux / macOS:
```bash
export GEMINI_API_KEY="your_gemini_api_key_here"
export GEMINI_MODEL="gemini-3.8-flash"
export DB_URL="jdbc:mysql://localhost:3306/ai_study_buddy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password_here"
export JWT_SECRET="your_secure_random_jwt_secret_at_least_32_characters_long"
export FRONTEND_ORIGIN="http://localhost:3000,http://localhost:8080"
```

### Windows Command Prompt (`cmd.exe`):
```cmd
set GEMINI_API_KEY=your_gemini_api_key_here
set GEMINI_MODEL=gemini-3.8-flash
set DB_URL=jdbc:mysql://localhost:3306/ai_study_buddy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
set DB_USERNAME=root
set DB_PASSWORD=your_mysql_password_here
set JWT_SECRET=your_secure_random_jwt_secret_at_least_32_characters_long
set FRONTEND_ORIGIN=http://localhost:3000,http://localhost:8080
```

### Windows PowerShell:
$env:GEMINI_API_KEY="your_gemini_api_key_here"
$env:GEMINI_MODEL="your_supported_gemini_model"
$env:DB_URL="jdbc:mysql://localhost:3306/ai_study_buddy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password_here"
$env:JWT_SECRET="your_random_secret_at_least_32_characters_long"
$env:FRONTEND_ORIGIN="http://localhost:3000,http://localhost:8080"
```

### IntelliJ IDEA Configuration:
1. Open the project in IntelliJ IDEA.
2. Navigate to **Run** -> **Edit Configurations...**
3. Under Spring Boot, select `AiStudyBuddyApplication`.
4. In the **Environment variables** field, enter your environment variables semicolon-delimited:
   `GEMINI_API_KEY=...;DB_URL=...;DB_USERNAME=root;DB_PASSWORD=...;JWT_SECRET=...`
5. Click **Apply** and **OK**.

---

## 5. Running the Application

### 1. Start MySQL & Initialize Database
```bash
mysql -u root -p < spring-boot-backend/src/main/resources/schema.sql
```

### 2. Start Spring Boot Backend
```bash
cd spring-boot-backend
mvn clean compile
mvn spring-boot:run
```
Backend runs on `http://localhost:8080`.

### 3. Run Automated Tests
```bash
mvn test
```

### 4. Serve Frontend
# Open another terminal from the project root
cd frontend

# Install dependencies, if not already installed
npm install

# Start the frontend
npm run dev

Open `http://localhost:3000` in your web browser.
