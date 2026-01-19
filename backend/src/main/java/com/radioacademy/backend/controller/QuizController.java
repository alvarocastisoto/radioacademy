package com.radioacademy.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.entity.Quiz;
import com.radioacademy.backend.service.exams.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<Quiz> createQuiz(@Valid @RequestBody QuizDTO quizDTO) {
        Quiz createdQuiz = quizService.createQuiz(quizDTO);
        return ResponseEntity.ok(createdQuiz);
    }
}
