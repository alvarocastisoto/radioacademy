package com.radioacademy.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 1. Obtenemos la ruta del proyecto
        String projectDir = System.getProperty("user.dir");

        // 2. Construimos la ruta "file:///" a mano para evitar errores de Windows
        // La estructura debe ser: file:///C:/Users/.../uploads/images/
        String uploadPath = "file:///" + projectDir + "/uploads/images/";

        // 3. ⚠️ CORRECCIÓN VITAL PARA WINDOWS:
        // Cambiamos las contrabarras (\) por barras normales (/)
        uploadPath = uploadPath.replace("\\", "/");

        // Nos aseguramos de que termine en / si o si
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        System.out.println("🌍 STATIC RESOURCES MAPEADOS A: " + uploadPath);

        // 4. Mapeamos
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(uploadPath);
    }
}