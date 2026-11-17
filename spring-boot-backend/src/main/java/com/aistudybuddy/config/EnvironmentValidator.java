package com.aistudybuddy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentValidator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentValidator.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String geminiModel;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    @Value("${spring.datasource.username:}")
    private String dbUsername;

    @Override
    public void run(String... args) {
        log.info("==========================================================");
        log.info("AI Study Buddy – Startup Environment Validation");
        log.info("==========================================================");

        // 1. Validate JWT Secret
  
    if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
    throw new IllegalStateException(
        "JWT_SECRET is missing. Configure a secure secret of at least 32 characters."
    );
    }

    if (jwtSecret.trim().length() < 32) {
    throw new IllegalStateException(
        "JWT_SECRET must contain at least 32 characters."
    );
    }

log.info("JWT_SECRET is configured.");     
  

log.info("JWT_SECRET is configured.");

        // 2. Validate Gemini AI Key
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("your_gemini")) {
            log.warn("! GEMINI_API_KEY is not configured or using default template placeholder.");
            log.warn("  AI features (Notes Summarizer, Quiz Generator, Doubt Solver) will fail until you provide a key.");
            log.warn("  Set GEMINI_API_KEY in your shell, IDE configuration, or environment.");
        } else {
            log.info("✓ GEMINI_API_KEY configured. Active Model: {}", geminiModel);
        }

        // 3. Database connection summary
        log.info("✓ MySQL Database Target: {}", dbUrl);
        log.info("✓ Database Username: {}", dbUsername);
        log.info("==========================================================");
    }
}
