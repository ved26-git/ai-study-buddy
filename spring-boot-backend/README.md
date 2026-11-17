# AI Study Buddy – Spring Boot REST API & Backend Documentation

Production-grade RESTful API backend built with **Java 17**, **Spring Boot 3.2**, **Spring Security 6 (Stateless JWT)**, **Spring Data JPA**, **Hibernate ORM**, **MySQL 8.0**, and **Google Gemini Generative AI**.

---

## 1. Architecture & Package Structure

```text
com.aistudybuddy
├── config
│   ├── EnvironmentValidator.java     # Startup validator for required environment variables (JWT_SECRET, GEMINI_API_KEY, DB_URL)
│   └── WebConfig.java                # CORS registry mapping and allowed HTTP methods
├── controller
│   ├── AuthController.java           # User registration, login, profile (/me, /profile), and logout
│   ├── ConfigController.java         # Public system configuration (active Gemini model, app version)
│   ├── DashboardController.java      # Authoritative database analytics & activity timeline
│   ├── DoubtSolverController.java    # AI academic doubt resolution & chat history
│   ├── HistoryController.java        # Aggregated learning records (Notes, Quizzes, Doubts)
│   ├── NotesController.java          # Lecture notes summarization & MySQL notes persistence
│   └── QuizController.java           # Quiz generation with concealed answers & server-side grading
├── service
│   ├── AuthService.java              # BCrypt hashing, JWT generation, profile updates, SecurityContext resolver
│   ├── DoubtSolverService.java       # Chat conversation persistence and retrieval
│   ├── GeminiService.java            # Google Gemini AI client with strict JSON schema validation
│   ├── HistoryService.java           # Analytics calculation with zero-division & NaN guards
│   ├── NotesService.java             # Notes summarization business logic
│   └── QuizService.java              # Server-authoritative quiz grading, duplicate-lock & safe DTOs
├── repository
│   ├── DoubtRepository.java          # Spring Data JPA repository for doubts table
│   ├── QuizRepository.java           # Spring Data JPA repository for quizzes table
│   ├── RevokedTokenRepository.java   # Spring Data JPA repository for persistent revoked_tokens table
│   ├── StudyNotesRepository.java     # Spring Data JPA repository for study_notes table
│   └── UserRepository.java           # Spring Data JPA repository for users table
├── entity
│   ├── Doubt.java                    # JPA Entity mapped to doubts table
│   ├── Quiz.java                     # JPA Entity mapped to quizzes table
│   ├── RevokedToken.java             # JPA Entity mapped to revoked_tokens table (SHA-256 hash + expiry)
│   ├── StudyNotes.java               # JPA Entity mapped to study_notes table
│   └── User.java                     # JPA Entity mapped to users table
├── dto
│   ├── validator
│   │   ├── ValidQuestionCount.java          # Bean Validation annotation restricting question count to 5, 10, 15
│   │   └── ValidQuestionCountValidator.java # ConstraintValidator implementation
│   ├── AuthDTOs.java                        # Register, Login, UpdateProfile, AuthResponse, UserSummary DTOs
│   └── StudyDTOs.java                       # QuizListResponse, QuizAttemptResponse, QuizSubmitResponse, etc.
├── security
│   ├── JwtAuthenticationFilter.java         # OncePerRequestFilter verifying tokens against blacklist & provider
│   ├── JwtTokenProvider.java                # HMAC-SHA256 JWT creation, extraction, strength validation (>=256 bits)
│   ├── SecurityConfig.java                  # Stateless security, exact CORS origins, sanitized 401/403 handlers
│   └── TokenBlacklistService.java           # Persistent token revocation (SHA-256 in MySQL + memory cache)
├── exception
│   ├── DoubtNotFoundException.java          # Custom exception for missing doubt records (HTTP 404)
│   ├── GlobalExceptionHandler.java          # Centralized REST error handling (400, 401, 403, 404, 409, 500)
│   ├── InvalidQuizException.java            # Custom exception for quiz validation failures (HTTP 400)
│   ├── NoteNotFoundException.java           # Custom exception for missing study notes (HTTP 404)
│   ├── QuizAlreadySubmittedException.java    # Custom exception for duplicate quiz submissions (HTTP 409)
│   ├── QuizNotFoundException.java           # Custom exception for missing quiz IDs (HTTP 404)
│   └── QuizValidationException.java         # Custom exception alias extending InvalidQuizException
└── AiStudyBuddyApplication.java             # Spring Boot application bootstrap entry point
```

---

## 2. Environment Variables & Configuration

All secrets, database credentials, and host settings are loaded exclusively from environment variables:

| Environment Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `GEMINI_API_KEY` | Google Gemini API Key from Google AI Studio | *(Required)* |
| `GEMINI_MODEL` | Gemini Model ID | `gemini-2.5-flash` |
| `DB_URL` | MySQL JDBC Connection URL | `jdbc:mysql://localhost:3306/ai_study_buddy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | *(Required in production)* |
| `JWT_SECRET` | HMAC-SHA256 Signing Key (min 32 bytes / 256 bits) | *(Required; validated on startup)* |
| `JWT_EXPIRATION_MS` | JWT validity duration in milliseconds | `604800000` (7 days) |
| `SERVER_PORT` | HTTP Server Port | `8080` |
| `FRONTEND_ORIGIN` | Comma-separated allowed CORS origins | `http://localhost:3000,http://localhost:5500,http://127.0.0.1:5500,http://localhost:8080` |

### IMPORTANT: Spring Boot Environment Variable Loading
> **Note**: Spring Boot does **not** natively load `.env` files automatically without third-party plugins. Environment variables must be set in your terminal shell, inside your IDE Run Configuration, or passed via container environment variables.

#### Setting Variables on Linux / macOS (Bash / Zsh):
```bash
export GEMINI_API_KEY="your_actual_gemini_api_key"
export GEMINI_MODEL="gemini-3.8-flash"
export DB_URL="jdbc:mysql://localhost:3306/ai_study_buddy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export JWT_SECRET="your_secure_random_jwt_secret_at_least_32_characters_long"
export FRONTEND_ORIGIN="http://localhost:3000,http://localhost:8080"
```

#### Setting Variables on Windows Command Prompt (`cmd.exe`):
```cmd
set GEMINI_API_KEY=your_actual_gemini_api_key
set GEMINI_MODEL=gemini-3.8-flash
set DB_URL=jdbc:mysql://localhost:3306/ai_study_buddy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
set DB_USERNAME=root
set DB_PASSWORD=your_mysql_password
set JWT_SECRET=your_secure_random_jwt_secret_at_least_32_characters_long
set FRONTEND_ORIGIN=http://localhost:3000,http://localhost:8080
```

#### Setting Variables on Windows PowerShell:
```powershell
$env:GEMINI_API_KEY="your_actual_gemini_api_key"
$env:GEMINI_MODEL="gemini-3.8-flash"
$env:DB_URL="jdbc:mysql://localhost:3306/ai_study_buddy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="your_secure_random_jwt_secret_at_least_32_characters_long"
$env:FRONTEND_ORIGIN="http://localhost:3000,http://localhost:8080"
```

#### Configuring in IntelliJ IDEA:
1. Open the project in IntelliJ IDEA.
2. Go to **Run** -> **Edit Configurations...**
3. Select `AiStudyBuddyApplication` under Spring Boot.
4. In the **Environment variables** field, click the browse icon and add:
   ```text
   GEMINI_API_KEY=your_actual_gemini_api_key;DB_URL=jdbc:mysql://localhost:3306/ai_study_buddy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true;DB_USERNAME=root;DB_PASSWORD=your_password;JWT_SECRET=your_secure_random_jwt_secret_at_least_32_characters_long
   ```
5. Click **Apply** and **OK**.

---

## 3. Quick Start & Execution

### Prerequisites
* **Java JDK**: 17 or later (`java -version`)
* **Maven**: 3.8 or later (`mvn -version`)
* **MySQL**: 8.0 or later running on port 3306 (`mysql -u root -p`)
* **Google Gemini API Key**: Free key from [Google AI Studio](https://aistudio.google.com/app/apikey)

### Running Locally
```bash
# 1. Navigate to backend directory
cd spring-boot-backend

# 2. Initialize MySQL Database and Indexes
mysql -u root -p < src/main/resources/schema.sql

# 3. Compile and run the Spring Boot application
mvn clean compile
mvn spring-boot:run

# 4. Execute automated test suite (uses embedded H2 database)
mvn test
```

The server starts at `http://localhost:8080`.

---

## 4. Comprehensive REST API Reference

All protected endpoints require an `Authorization` header containing the JWT token received upon login or registration:
```http
Authorization: Bearer <your_jwt_token>
```

---

### A. System Configuration (`/api/config`)

#### 1. Get Application Configuration
* **Method**: `GET`
* **URL**: `/api/config`
* **Authentication**: Public
* **Success Response (`200 OK`)**:
  ```json
  {
    "geminiModel": "gemini-3.8-flash",
    "appName": "AI Study Buddy",
    "version": "1.0.0"
  }
  ```

---

### B. Authentication Endpoints (`/api/auth`)

#### 1. Register Student Account
* **Method**: `POST`
* **URL**: `/api/auth/register`
* **Authentication**: Public
* **Request Body**:
  ```json
  {
    "name": "Alex Johnson",
    "email": "alex@aistudybuddy.com",
    "password": "Password123!",
    "profilePicture": "/assets/images/student_avatar_user_1794203529926.jpg"
  }
  ```
* **Success Response (`201 Created`)**:
  ```json
  {
    "message": "Registration successful",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "userId": 1,
      "name": "Alex Johnson",
      "email": "alex@aistudybuddy.com",
      "profilePicture": "/assets/images/student_avatar_user_1794203529926.jpg"
    }
  }
  ```

#### 2. Student Login
* **Method**: `POST`
* **URL**: `/api/auth/login`
* **Authentication**: Public
* **Request Body**:
  ```json
  {
    "email": "alex@aistudybuddy.com",
    "password": "Password123!"
  }
  ```
* **Success Response (`200 OK`)**:
  ```json
  {
    "message": "Login successful",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "userId": 1,
      "name": "Alex Johnson",
      "email": "alex@aistudybuddy.com",
      "profilePicture": "/assets/images/student_avatar_user_1794203529926.jpg"
    }
  }
  ```

#### 3. Get Current User Profile
* **Method**: `GET`
* **URL**: `/api/auth/me`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Success Response (`200 OK`)**:
  ```json
  {
    "userId": 1,
    "name": "Alex Johnson",
    "email": "alex@aistudybuddy.com",
    "profilePicture": "/assets/images/student_avatar_user_1794203529926.jpg"
  }
  ```

#### 4. Update Profile
* **Method**: `PUT`
* **URL**: `/api/auth/profile`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Request Body**:
  ```json
  {
    "name": "Alex Johnson Updated",
    "profilePicture": "/assets/images/student_avatar_male_1794205455718.jpg"
  }
  ```
* **Success Response (`200 OK`)**:
  ```json
  {
    "userId": 1,
    "name": "Alex Johnson Updated",
    "email": "alex@aistudybuddy.com",
    "profilePicture": "/assets/images/student_avatar_male_1794205455718.jpg"
  }
  ```

#### 5. Server-Side Logout & Persistent Token Revocation
* **Method**: `POST`
* **URL**: `/api/auth/logout`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Success Response (`200 OK`)**:
  ```json
  {
    "message": "Logged out successfully and token invalidated"
  }
  ```
* **Security Behavior**: Hashes the incoming JWT using SHA-256 and persists it to MySQL `revoked_tokens` table while caching in memory. Survives application restarts.

---

### C. Notes Summarizer Endpoints (`/api/notes`)

#### 1. Summarize Notes with Gemini AI
* **Method**: `POST`
* **URL**: `/api/notes/summarize`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Request Body**:
  ```json
  {
    "title": "Operating Systems: Deadlock & Prevention",
    "content": "A deadlock occurs when processes are waiting for resources held by each other..."
  }
  ```
* **Success Response (`200 OK`)**:
  ```json
  {
    "noteId": 14,
    "title": "Operating Systems: Deadlock & Prevention",
    "originalContent": "A deadlock occurs when processes are waiting for resources...",
    "summary": "### Core Concept & Overview\nA Deadlock occurs when processes hold resources...\n\n### Key Points & Definitions\n* Mutual Exclusion\n* Hold and Wait\n* No Preemption\n* Circular Wait",
    "createdAt": "2026-11-09T08:30:00"
  }
  ```

#### 2. Get Notes History
* **Method**: `GET`
* **URL**: `/api/notes`
* **Authentication**: Protected (`Bearer <JWT>`)

#### 3. Delete Note
* **Method**: `DELETE`
* **URL**: `/api/notes/{id}`
* **Authentication**: Protected (`Bearer <JWT>`)

---

### D. Quiz Generator & Scoring Endpoints (`/api/quiz`)

#### 1. Generate Quiz (Concealed Answer Key)
* **Method**: `POST`
* **URL**: `/api/quiz/generate`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Request Body**:
  ```json
  {
    "topic": "Java Object-Oriented Programming (OOP)",
    "numQuestions": 5,
    "difficulty": "Medium"
  }
  ```
* **Success Response (`201 Created`)** – **`QuizAttemptResponse`**:
  *Answer keys and explanations are strictly withheld until submission!*
  *AI generation guarantees exactly requested question count, 4 distinct non-empty options per question, and verified matching answers.*
  ```json
  {
    "quizId": 25,
    "topic": "Java Object-Oriented Programming (OOP)",
    "difficulty": "Medium",
    "totalQuestions": 5,
    "createdAt": "2026-11-09T08:35:00",
    "questions": [
      {
        "index": 0,
        "question": "Which of the following is NOT an OOP principle in Java?",
        "options": ["Encapsulation", "Polymorphism", "Compilation", "Inheritance"]
      }
    ]
  }
  ```

#### 2. Submit Quiz for Authoritative Server-Side Grading
* **Method**: `POST`
* **URL**: `/api/quiz/{id}/submit`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Security Protections**:
  - **Duplicate Lock**: Rejects submission if `completedAt` or `score` is already present with HTTP 400.
  - **Index Validation**: Enforces `0 <= index < totalQuestions`. Rejects negative or out-of-bounds keys.
  - **Answer Validation**: Rejects empty or blank answer strings.
* **Request Body**:
  ```json
  {
    "answers": {
      "0": "Compilation",
      "1": "final",
      "2": "The Diamond Problem",
      "3": "Comparable",
      "4": "public"
    }
  }
  ```
* **Success Response (`200 OK`)** – **`QuizSubmitResponse`**:
  ```json
  {
    "quizId": 25,
    "score": 5,
    "totalQuestions": 5,
    "percentage": 100,
    "questions": [
      {
        "question": "Which of the following is NOT an OOP principle in Java?",
        "options": ["Encapsulation", "Polymorphism", "Compilation", "Inheritance"],
        "correctAnswer": "Compilation",
        "explanation": "Compilation is a translation process, whereas Encapsulation, Polymorphism, Inheritance, and Abstraction are the four OOP principles."
      }
    ],
    "submittedAnswers": {
      "0": "Compilation",
      "1": "final",
      "2": "The Diamond Problem",
      "3": "Comparable",
      "4": "public"
    }
  }
  ```

#### 3. List Student Quizzes
* **Method**: `GET`
* **URL**: `/api/quiz` or `/api/quiz/history`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Returns**: `List<QuizListResponse>` (score, topic, difficulty, completedAt; raw questions JSON is never leaked).

#### 4. Get Quiz Details by ID
* **Method**: `GET`
* **URL**: `/api/quiz/{id}`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Returns**: `QuizAttemptResponse` if unsubmitted, or `QuizSubmitResponse` if graded.

---

### E. AI Doubt Solver Endpoints (`/api/doubts`)

#### 1. Ask Academic Doubt
* **Method**: `POST`
* **URL**: `/api/doubts/ask`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Request Body**:
  ```json
  {
    "question": "What is the difference between Comparable and Comparator in Java?"
  }
  ```
* **Success Response (`200 OK`)**:
  ```json
  {
    "doubtId": 8,
    "question": "What is the difference between Comparable and Comparator in Java?",
    "answer": "### Comparable vs. Comparator in Java\n\n* **Comparable**: Used for natural sorting...\n* **Comparator**: Used for custom multiple sorting...",
    "createdAt": "2026-11-09T08:40:00"
  }
  ```

#### 2. Get Doubt History
* **Method**: `GET`
* **URL**: `/api/doubts/history`
* **Authentication**: Protected (`Bearer <JWT>`)

#### 3. Clear All Doubts
* **Method**: `DELETE`
* **URL**: `/api/doubts/clear`
* **Authentication**: Protected (`Bearer <JWT>`)

---

### F. Dashboard & Analytics Endpoints (`/api/dashboard`, `/api/history`)

#### 1. Real-Time Dashboard Statistics
* **Method**: `GET`
* **URL**: `/api/dashboard/stats`
* **Authentication**: Protected (`Bearer <JWT>`)
* **Success Response (`200 OK`)**:
  ```json
  {
    "totalSummaries": 12,
    "totalQuizzes": 7,
    "averageQuizScore": 88,
    "totalDoubts": 5
  }
  ```
* **Guarantees**: If no completed quizzes exist, returns `0%`. Never returns `NaN` or `Infinity`.

#### 2. Recent Activity Timeline
* **Method**: `GET`
* **URL**: `/api/dashboard/recent-activities`
* **Authentication**: Protected (`Bearer <JWT>`)

#### 3. Aggregated Study History
* **Method**: `GET`
* **URL**: `/api/history`
* **Authentication**: Protected (`Bearer <JWT>`)

---

## 5. Automated Testing Suite

The backend contains automated tests utilizing **JUnit 5**, **Mockito**, **MockMvc**, and **H2 In-Memory Database**:

| Test Class | Scope | What It Verifies |
| :--- | :--- | :--- |
| `AuthServiceTest` | Unit | Registration, duplicate email rejection, BCrypt matching, JWT generation, profile updates, and token blacklisting on logout. |
| `TokenBlacklistServiceTest` | Unit | Persistent token revocation in MySQL, sub-millisecond local cache retrieval, database lookup on cache miss (survives restart), SHA-256 hash determinism, and expired token cleanup. |
| `QuizServiceTest` | Unit | Generation of sanitized `QuizAttemptResponse` (answers concealed), server-side score calculation, duplicate submission prevention, question index & blank answer validation, server-side validation of exactly 4 options, correctAnswer matching, and malformed JSON rejection. |
| `GeminiQuizValidationTest` | Unit | Strict validation of Gemini AI JSON: exact question counts, exactly 4 distinct non-empty options, non-blank answers matching options, and malformed JSON rejection. |
| `DashboardStatisticsTest` | Unit | Real MySQL count queries, zero division guards, 0% empty state, safe averaging (no NaN/Infinity). |
| `NotesServiceTest` | Unit | Note summarization, title generation, and MySQL persistence. |
| `DoubtSolverServiceTest` | Unit | Step-by-step doubt resolution and conversation persistence. |
| `AuthControllerIntegrationTest` | MockMvc | `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, `/api/auth/profile`, and `/api/auth/logout`. |
| `SecurityIntegrationTest` | MockMvc | Enforces HTTP 401 Unauthorized with sanitized JSON on protected routes without a valid Bearer token. |

---

## 6. Database Schema & Migration Strategy

The database schema is defined in `src/main/resources/schema.sql` and includes production-grade indexes:

```sql
-- Composite performance indexes
CREATE INDEX idx_study_notes_user_created ON study_notes (user_id, created_at DESC);
CREATE INDEX idx_quizzes_user_created ON quizzes (user_id, created_at DESC);
CREATE INDEX idx_quizzes_user_score ON quizzes (user_id, score);
CREATE INDEX idx_doubts_user_created ON doubts (user_id, created_at DESC);
CREATE INDEX idx_users_email ON users (email);

-- Persistent token revocation table
CREATE TABLE IF NOT EXISTS revoked_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    expiry_date DATETIME NOT NULL,
    revoked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_revoked_tokens_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Migration Strategy**:
- For new installations: Execute `schema.sql` directly on your MySQL instance.
- For existing installations: The schema utilizes `CREATE TABLE IF NOT EXISTS` and non-destructive index declarations, preserving existing user data.
