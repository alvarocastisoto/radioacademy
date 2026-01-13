package com.radioacademy.backend.listener;

import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.event.PasswordResetEvent;
import com.radioacademy.backend.event.UserRegistrationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class RegistrationListener {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Async
    @EventListener
    public void handleUserRegistration(UserRegistrationEvent event) {
        User user = event.getUser();
        System.out.println("📨 Iniciando envío de correo NATIVO a: " + user.getEmail());

        try {
            // 1. Construimos el JSON a mano (Rápido y sucio, pero funciona)
            // OJO: En producción usaríamos Jackson, pero esto es para que te funcione YA.
            String htmlContent = "<h1>¡Bienvenido a RadioAcademy, " + user.getName()
                    + "!</h1><p>Gracias por registrarte.</p>";

            // Escapamos las comillas dobles en el HTML por si acaso
            String safeHtml = htmlContent.replace("\"", "\\\"");

            String jsonBody = String.format("""
                    {
                        "from": "onboarding@resend.dev",
                        "to": ["%s"],
                        "subject": "Bienvenido a RadioAcademy 🎙️",
                        "html": "%s"
                    }
                    """, user.getEmail(), safeHtml);

            // 2. Creamos la petición HTTP
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 3. Enviamos
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                System.out.println("✅ Correo enviado con éxito. Respuesta: " + response.body());
            } else {
                System.err.println("❌ Fallo al enviar. Status: " + response.statusCode());
                System.err.println("❌ Error: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("❌ Error crítico enviando correo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    @EventListener
    public void handlePasswordReset(PasswordResetEvent event) {
        User user = event.getUser();
        String resetUrl = "http://localhost:4200/reset-password?token=" + event.getToken();

        System.out.println("🔑 Enviando email de recuperación a: " + user.getEmail());

        try {
            String htmlContent = "<h1>Recuperar Contraseña</h1>" +
                    "<p>Hola " + user.getName() + ", has solicitado restablecer tu contraseña.</p>" +
                    "<p>Haz clic en el siguiente enlace (válido por 15 min):</p>" +
                    "<a href=\"" + resetUrl + "\">Restablecer mi contraseña</a>" +
                    "<p>Si no fuiste tú, ignora este correo.</p>";

            String safeHtml = htmlContent.replace("\"", "\\\"");

            String jsonBody = String.format("""
                    {
                        "from": "onboarding@resend.dev",
                        "to": ["%s"],
                        "subject": "Recuperación de Contraseña 🔐",
                        "html": "%s"
                    }
                    """, user.getEmail(), safeHtml);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("✅ Email de recuperación enviado.");

        } catch (Exception e) {
            System.err.println("❌ Error enviando email de recuperación: " + e.getMessage());
        }
    }
}