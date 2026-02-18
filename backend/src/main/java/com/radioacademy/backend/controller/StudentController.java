package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.course.CourseContentDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.dto.exams.QuizResultDTO;
import com.radioacademy.backend.dto.exams.QuizSubmissionDTO;
import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.student.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor 
public class StudentController {

        private final StudentService studentService;

        
        @GetMapping("/dashboard")
        public ResponseEntity<List<CourseDashboardDTO>> getMyDashboard(
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                return ResponseEntity.ok(studentService.getMyDashboard(userDetails));
        }

        
        @GetMapping("/course/{courseId}/content")
        public ResponseEntity<CourseContentDTO> getCourseContent(
                        @PathVariable UUID courseId,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                return ResponseEntity.ok(studentService.getCourseContent(courseId, userDetails));
        }

        
        @GetMapping("/quiz/{quizId}")
        public ResponseEntity<QuizDTO> getQuizForStudent(@PathVariable UUID quizId) {
                return ResponseEntity.ok(studentService.getQuizForStudent(quizId));
        }

        
        @PostMapping("/quizzes/submit") 
        public ResponseEntity<QuizResultDTO> submitQuiz(
                        @RequestBody QuizSubmissionDTO submission,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                
                return ResponseEntity.ok(studentService.submitQuiz(submission, userDetails));
        }
}