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

        // 1. Obtenemos la ruta absoluta de la carpeta "uploads" en la raíz del proyecto
        // Nota: Apuntamos a "uploads", no a "uploads/images", para tener flexibilidad
        String projectRoot = System.getProperty("user.dir");
        String uploadPath = Paths.get(projectRoot, "uploads").toUri().toString();

        System.out.println("🌍 MAPEANDO RECURSOS ESTÁTICOS A: " + uploadPath);

        // 2. Configuración del mapeo
        // Cuando alguien pida /uploads/images/foto.jpg -> Busca en
        // {Proyecto}/uploads/images/foto.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}