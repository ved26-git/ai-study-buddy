package com.aistudybuddy.controller;

import com.aistudybuddy.service.GeminiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final GeminiService geminiService;

    @Value("${spring.application.name:ai-study-buddy}")
    private String appName;

    public ConfigController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Public system configuration endpoint exposing dynamic runtime settings like GEMINI_MODEL.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "appName", appName,
                "geminiModel", geminiService.getModel(),
                "version", "1.0.0",
                "status", "UP"
        ));
    }
}
