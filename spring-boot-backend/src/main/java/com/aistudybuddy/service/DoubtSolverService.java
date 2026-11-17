package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.DoubtResponse;
import com.aistudybuddy.entity.Doubt;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.repository.DoubtRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoubtSolverService {

    private final DoubtRepository doubtRepository;
    private final GeminiService geminiService;

    public DoubtSolverService(DoubtRepository doubtRepository, GeminiService geminiService) {
        this.doubtRepository = doubtRepository;
        this.geminiService = geminiService;
    }

    @Transactional
    public DoubtResponse solveDoubt(User user, String question) {
        String answer = geminiService.solveDoubt(question);

        Doubt doubt = new Doubt(user, question, answer);
        Doubt saved = doubtRepository.save(doubt);

        return new DoubtResponse(
                saved.getDoubtId(),
                saved.getQuestion(),
                saved.getAnswer(),
                saved.getCreatedAt().toString()
        );
    }

    public List<DoubtResponse> getUserDoubts(User user) {
        return doubtRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(d -> new DoubtResponse(d.getDoubtId(), d.getQuestion(), d.getAnswer(), d.getCreatedAt().toString()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDoubt(Long doubtId, User user) {
        Doubt doubt = doubtRepository.findByDoubtIdAndUser(doubtId, user)
                .orElseThrow(() -> new com.aistudybuddy.exception.DoubtNotFoundException("Doubt record #" + doubtId + " not found or access denied."));
        doubtRepository.delete(doubt);
    }

    @Transactional
    public void clearUserDoubts(User user) {
        doubtRepository.deleteByUser(user);
    }
}
