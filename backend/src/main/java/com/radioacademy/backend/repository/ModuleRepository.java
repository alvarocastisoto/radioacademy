package com.radioacademy.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import com.radioacademy.backend.entity.Module;
import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, UUID> {
    List<Module> findByCourseIdOrderByOrderIndexAsc(UUID courseId);
}
