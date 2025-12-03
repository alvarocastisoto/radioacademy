package com.radioacademy.backend.repository;

import com.radioacademy.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByActiveTrue();

}
