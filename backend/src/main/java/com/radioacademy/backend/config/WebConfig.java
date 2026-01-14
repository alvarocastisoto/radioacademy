package com.radioacademy.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String projectDir = System.getProperty("user.dir");

        // 1. CONFIGURACIÓN IMÁGENES
        String imagesPath = "file:///" + projectDir + "/uploads/images/";
        imagesPath = imagesPath.replace("\\", "/");
        if (!imagesPath.endsWith("/"))
            imagesPath += "/";

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(imagesPath);

        System.out.println("🌍 STATIC IMAGES MAPEADAS A: " + imagesPath);

        // 2. CONFIGURACIÓN PDFs (✅ NUEVO)
        String pdfsPath = "file:///" + projectDir + "/uploads/pdfs/";
        pdfsPath = pdfsPath.replace("\\", "/");
        if (!pdfsPath.endsWith("/"))
            pdfsPath += "/";

        registry.addResourceHandler("/uploads/pdfs/**")
                .addResourceLocations(pdfsPath);

        System.out.println("🌍 STATIC PDFS MAPEADOS A: " + pdfsPath);
    }
}