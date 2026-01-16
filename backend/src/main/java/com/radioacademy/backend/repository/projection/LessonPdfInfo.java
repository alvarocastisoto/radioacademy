// backend/src/main/java/com/radioacademy/backend/repository/projection/LessonPdfInfo.java
package com.radioacademy.backend.repository.projection;

import java.util.UUID;

public interface LessonPdfInfo {
    UUID getCourseId();

    String getPdfUrl();
}
