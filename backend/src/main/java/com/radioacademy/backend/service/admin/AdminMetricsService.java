package com.radioacademy.backend.service.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.radioacademy.backend.dto.metrics.DailyMetricDTO;
import com.radioacademy.backend.dto.metrics.DailyRevenueDTO;
import com.radioacademy.backend.dto.metrics.TopCourseDTO;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMetricsService {

    private final JdbcTemplate jdbc;

    public List<DailyMetricDTO> registeredDaily(int days) {
        String sql = """
                    SELECT date_trunc('day', created_at) AS day, COUNT(*) AS registered
                    FROM users
                    WHERE created_at >= now() - (? || ' days')::interval
                    GROUP BY 1
                    ORDER BY 1
                """;
        return jdbc.query(sql,
                (rs, i) -> new DailyMetricDTO(rs.getTimestamp("day").toString(), rs.getLong("registered")),
                days);
    }

    public List<DailyMetricDTO> enrollmentsDaily(int days) {
        String sql = """
                    SELECT date_trunc('day', enrolled_at) AS day, COUNT(*) AS enrollments
                    FROM enrollments
                    WHERE enrolled_at >= now() - (? || ' days')::interval
                    GROUP BY 1
                    ORDER BY 1
                """;
        return jdbc.query(sql,
                (rs, i) -> new DailyMetricDTO(rs.getTimestamp("day").toString(), rs.getLong("enrollments")),
                days);
    }

    public List<DailyRevenueDTO> revenueDaily(int days) {
        String sql = """
                    SELECT date_trunc('day', enrolled_at) AS day, COALESCE(SUM(amount_paid), 0) AS revenue
                    FROM enrollments
                    WHERE enrolled_at >= now() - (? || ' days')::interval
                    GROUP BY 1
                    ORDER BY 1
                """;
        return jdbc.query(sql,
                (rs, i) -> new DailyRevenueDTO(rs.getTimestamp("day").toString(),
                        rs.getBigDecimal("revenue").toPlainString()),
                days);
    }

    public List<TopCourseDTO> topCourses(int limit) {
        String sql = """
                    SELECT course_id, COUNT(*) AS enrollments, COALESCE(SUM(amount_paid), 0) AS revenue
                    FROM enrollments
                    GROUP BY course_id
                    ORDER BY enrollments DESC
                    LIMIT ?
                """;
        return jdbc.query(sql, (rs, i) -> new TopCourseDTO(
                rs.getObject("course_id", UUID.class),
                rs.getLong("enrollments"),
                rs.getBigDecimal("revenue").toPlainString()),
                limit);
    }
}