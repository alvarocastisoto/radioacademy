package com.radioacademy.backend.controller;

import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.student.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentLessonPdfController {

    private final StudentService studentService;

    @GetMapping("/lessons/{lessonId}/pdf")
    public ResponseEntity<Resource> getLessonPdf(
            @PathVariable UUID lessonId,
            @RequestParam(defaultValue = "false") boolean download,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        
        Resource resource = studentService.getLessonPdf(lessonId, userDetails);

        
        String filename = inferFilename(resource, lessonId);
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + safeFilename(filename) + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .cacheControl(CacheControl.noStore()) 
                .body(resource);
    }

    

    private String inferFilename(Resource resource, UUID lessonId) {
        try {
            
            if (resource.getFilename() != null && !resource.getFilename().isBlank()) {
                return resource.getFilename();
            }
            
            
            
            
        } catch (Exception ignored) {
        }

        return "lesson-" + lessonId + ".pdf"; 
    }

    private String safeFilename(String name) {
        
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}