package com.radioacademy.backend.repository;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    
    List<Enrollment> findByUserId(UUID userId);

    
    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    
    
    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    void deleteByUser_IdAndCourse_Id(UUID userId, UUID courseId);

    @Query("select e.course from Enrollment e where e.user.id = :userId")
    List<Course> findCoursesByUserId(@Param("userId") UUID userId);

}