package com.radioacademy.backend.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.dto.exams.QuizResultDTO;
import com.radioacademy.backend.dto.exams.QuizSubmissionDTO;
import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.exams.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    
    @PostMapping
    public ResponseEntity<QuizDTO> createQuiz(@Valid @RequestBody QuizDTO quizDTO) {
        
        return ResponseEntity.ok(quizService.createQuiz(quizDTO));
    }

    
    
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<QuizDTO> getQuizByModule(@PathVariable UUID moduleId) {
        return quizService.getQuizByModuleId(moduleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<QuizDTO> getQuizById(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    
    @PostMapping("/submit")
    public ResponseEntity<QuizResultDTO> submitQuiz(@RequestBody QuizSubmissionDTO submission,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(quizService.submitQuiz(submission, userDetails));
    }

    
    
    @GetMapping("/{id}/smart-retry")
    public ResponseEntity<QuizDTO> getSmartFailedQuiz(@PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(quizService.getSmartFailedQuiz(id, userDetails));
    }
}