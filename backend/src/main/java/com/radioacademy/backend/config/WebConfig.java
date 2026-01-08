package com.radioacademy.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 1. Obtenemos la ruta absoluta
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();

        // ⚠️ EL FIX IMPORTANTE:
        // Si la ruta no termina en barra, se la añadimos.
        // Sin esto, Spring a veces piensa que "uploads" es un archivo y no una carpeta.
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        System.out.println("📂 Sirviendo archivos estáticos desde: " + uploadPath);
    }
}