package com.radioacademy.backend.repository;

import com.radioacademy.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByActiveTrue();

    ;
}
