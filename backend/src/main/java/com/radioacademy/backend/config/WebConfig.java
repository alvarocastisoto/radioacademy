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
        // Opción A: Ruta absoluta robusta (Recomendada)
        // Esto convierte C:\Proyectos\RadioAcademy\loads a
        // file:///C:/Proyectos/RadioAcademy/uploads/ automáticamente
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        System.out.println("📂 Sirviendo archivos estáticos desde: " + uploadPath);
    }
}