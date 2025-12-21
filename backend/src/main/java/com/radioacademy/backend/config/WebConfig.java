package com.radioacademy.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Obtenemos la ruta absoluta de la carpeta donde estás ejecutando el proyecto
        String rootPath = Paths.get(".").toAbsolutePath().normalize().toString();

        // Le añadimos el protocolo "file:///" que requiere Windows/Linux para ser feliz
        String uploadPath = "file:///" + rootPath + "/uploads/";

        // Configuración final
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        // Debug para que veas en consola dónde está buscando
        System.out.println("📂 Sirviendo archivos estáticos desde: " + uploadPath);
    }
}