package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.metrics.DailyMetricDTO;
import com.radioacademy.backend.dto.metrics.DailyRevenueDTO;
import com.radioacademy.backend.dto.metrics.TopCourseDTO;
import com.radioacademy.backend.service.admin.AdminMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final AdminMetricsService metrics;

    @GetMapping("/registered-daily")
    public List<DailyMetricDTO> registeredDaily(@RequestParam(defaultValue = "30") int days) {
        return metrics.registeredDaily(days);
    }

    @GetMapping("/enrollments-daily")
    public List<DailyMetricDTO> enrollmentsDaily(@RequestParam(defaultValue = "30") int days) {
        return metrics.enrollmentsDaily(days);
    }

    @GetMapping("/revenue-daily")
    public List<DailyRevenueDTO> revenueDaily(@RequestParam(defaultValue = "30") int days) {
        return metrics.revenueDaily(days);
    }

    @GetMapping("/top-courses")
    public List<TopCourseDTO> topCourses(@RequestParam(defaultValue = "10") int limit) {
        return metrics.topCourses(limit);
    }
}
