package com.radioacademy.backend.listener;

import com.fasterxml.jackson.databind.ObjectMapper; 
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.event.PasswordResetEvent;
import com.radioacademy.backend.event.UserRegistrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Component
public class RegistrationListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationListener.class);

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    @Autowired
    private ObjectMapper objectMapper; 

    
    private final HttpClient client = HttpClient.newHttpClient();

    
    
    
    @Async
    @EventListener
    public void handleUserRegistration(UserRegistrationEvent event) {
        User user = event.getUser();
        logger.info("📨 Enviando bienvenida a: {}", user.getEmail());

        String htmlContent = "<h1>¡Bienvenido a RadioAcademy, " + user.getName() + "!</h1>" +
                "<p>Gracias por registrarte.</p>";

        sendEmail(user.getEmail(), "Bienvenido a RadioAcademy 🎙️", htmlContent);
    }

    
    
    
    @Async
    @EventListener
    public void handlePasswordReset(PasswordResetEvent event) {
        User user = event.getUser();
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + event.getToken();

        logger.info("🔑 Enviando token recuperación a: {}", user.getEmail());

        String htmlContent = String.format("""
                <h1>Recuperar Contraseña</h1>
                <p>Hola %s, has solicitado restablecer tu contraseña.</p>
                <p>Haz clic en el siguiente enlace (válido por 15 min):</p>
                <a href="%s">Restablecer mi contraseña</a>
                <p>Si no fuiste tú, ignora este correo.</p>
                """, user.getName(), resetUrl);

        sendEmail(user.getEmail(), "Recuperación de Contraseña 🔐", htmlContent);
    }

    
    
    
    private void sendEmail(String to, String subject, String html) {
        try {
            
            Map<String, Object> emailData = Map.of(
                    "from", "onboarding@resend.dev",
                    "to", new String[] { to }, 
                    "subject", subject,
                    "html", html);

            String jsonBody = objectMapper.writeValueAsString(emailData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            logger.info("✅ Email enviado correctamente a {}", to);
                        } else {
                            logger.error("❌ Error enviando email. Status: {}. Body: {}", response.statusCode(),
                                    response.body());
                        }
                    });

        } catch (Exception e) {
            logger.error("❌ Excepción crítica al enviar email", e);
        }
    }
}