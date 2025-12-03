package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CreateCourseRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CreateCourseRequest request) {
        Optional<User> teacherOptional = userRepository.findById(request.teacherId());

        if (teacherOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Course newCourse = new Course();
        newCourse.setTitle(request.title());
        newCourse.setDescription(request.description());
        newCourse.setPrice(request.price());
        newCourse.setHours(request.hours());
        // ELIMINADO: newCourse.setLevel(request.level());
        newCourse.setActive(true);
        newCourse.setTeacher(teacherOptional.get());

        Course savedCourse = courseRepository.save(newCourse);
        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }
}