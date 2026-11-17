package com.aistudybuddy.controller;

import com.aistudybuddy.dto.StudyDTOs.DashboardStatsResponse;
import com.aistudybuddy.dto.StudyDTOs.RecentActivityDTO;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import com.aistudybuddy.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final HistoryService historyService;
    private final AuthService authService;

    public DashboardController(HistoryService historyService, AuthService authService) {
        this.historyService = historyService;
        this.authService = authService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        User user = authService.getCurrentAuthenticatedUser();
        DashboardStatsResponse stats = historyService.getDashboardStats(user);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent-activities")
    public ResponseEntity<List<RecentActivityDTO>> getRecentActivities() {
        User user = authService.getCurrentAuthenticatedUser();
        List<RecentActivityDTO> activities = historyService.getRecentActivities(user);
        return ResponseEntity.ok(activities);
    }
}
