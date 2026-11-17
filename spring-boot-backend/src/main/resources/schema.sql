-- ========================================================
-- AI Study Buddy: Production MySQL Database Schema
-- Database Name: ai_study_buddy
-- Storage Engine: InnoDB | Character Set: utf8mb4
-- ========================================================

CREATE DATABASE IF NOT EXISTS ai_study_buddy
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ai_study_buddy;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_picture LONGTEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. StudyNotes Table
CREATE TABLE IF NOT EXISTS study_notes (
    note_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    original_content LONGTEXT NOT NULL,
    summary LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_studynotes_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_studynotes_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Quizzes Table (Stores questions JSON & Server Graded Scores)
CREATE TABLE IF NOT EXISTS quizzes (
    quiz_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) DEFAULT 'Medium',
    questions LONGTEXT NOT NULL, -- JSON array of question, options, correctAnswer, explanation
    score INT DEFAULT NULL,
    total_questions INT NOT NULL DEFAULT 5,
    submitted_answers LONGTEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_quiz_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_quizzes_user_created (user_id, created_at DESC),
    INDEX idx_quizzes_user_score (user_id, score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Doubt Solving Chat History Table
CREATE TABLE IF NOT EXISTS doubts (
    doubt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question LONGTEXT NOT NULL,
    answer LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doubt_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_doubts_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Revoked Tokens Table (Persistent JWT Revocation on Server Logout)
CREATE TABLE IF NOT EXISTS revoked_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_revoked_token_hash (token_hash),
    INDEX idx_revoked_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initial Demo User (Password: password123 hashed via BCrypt)
INSERT INTO users (user_id, name, email, password)
VALUES (
    1,
    'Alex Johnson',
    'alex@aistudybuddy.com',
    '$2a$10$wK6P4/4v43aP6tZvh3mI..710N50k4FeqhNq.l06nL7Zqg71G2P6i'
) ON DUPLICATE KEY UPDATE name=name;
