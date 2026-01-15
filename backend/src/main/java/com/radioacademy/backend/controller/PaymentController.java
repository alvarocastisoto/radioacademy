package com.radioacademy.backend.controller;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository; // Solo para validación previa si quieres
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.EnrollmentService; // 👈 NUEVO IMPORT
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository; // Lo mantenemos solo para el check rápido en /checkout

    @Autowired
    private EnrollmentService enrollmentService; // 👈 INYECTAMOS EL SERVICIO

    // ... (Tu método getAuthenticatedUser sigue igual) ...
    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    // 1. CHECKOUT (Igual que antes, protegemos el intento de pago)
    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession(@RequestBody Map<String, String> request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Stripe.apiKey = stripeApiKey;

        UUID courseId = UUID.fromString(request.get("courseId"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        // Bloqueo rápido antes de llamar a Stripe
        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya tienes este curso."));
        }

        try {
            long amountInCents = course.getPrice().multiply(new BigDecimal(100)).longValue();

            // ... (Configuración de SessionCreateParams igual que tenías) ...
            // RESUMIDO PARA NO REPETIR CÓDIGO INNECESARIO:
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(user.getId().toString())
                    .putMetadata("course_id", course.getId().toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("eur")
                                    .setUnitAmount(amountInCents)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(course.getTitle()).build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);
            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 2. CONFIRMAR PAGO - AHORA USANDO EL SERVICIO
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, String> request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Stripe.apiKey = stripeApiKey;
        String sessionId = request.get("session_id");

        try {
            Session session = Session.retrieve(sessionId);

            if ("paid".equals(session.getPaymentStatus())) {
                String courseIdStr = session.getMetadata().get("course_id");
                Course course = courseRepository.findById(UUID.fromString(courseIdStr))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

                // 👈 AQUÍ LLAMAMOS AL SERVICIO NUEVO
                // El servicio se encarga de verificar si existe y guardar si no.
                enrollmentService.enrollUser(user, course, session.getPaymentIntent());

                return ResponseEntity.ok(Map.of("message", "Pago confirmado y curso activado"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Pago no completado"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al confirmar: " + e.getMessage()));
        }
    }
}