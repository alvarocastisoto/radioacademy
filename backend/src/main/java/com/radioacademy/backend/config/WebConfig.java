package com.radioacademy.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadsDir = Paths.get(System.getProperty("user.dir"), "uploads")
                .toAbsolutePath().normalize();

        String uploadsUri = uploadsDir.toUri().toString();
        if (!uploadsUri.endsWith("/"))
            uploadsUri += "/";

        // Solo imágenes
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(uploadsUri + "images/");
    }
}
