package com.radioacademy.backend.service.payment;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.content.EnrollmentService;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService; // 👈 Conectamos con el servicio de matrículas

    // ✅ Inicializa Stripe una sola vez al arrancar la app
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    // 1. CREAR SESIÓN DE PAGO
    public String createCheckoutSession(String userEmail, UUID courseId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        // Regla de Negocio: No pagar dos veces por lo mismo
        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya tienes este curso.");
        }

        try {
            long amountInCents = course.getPrice().multiply(new BigDecimal(100)).longValue();

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
                                            .setName(course.getTitle())
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();

        } catch (Exception e) {
            // Convertimos errores de Stripe en errores HTTP manejables
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error con Stripe: " + e.getMessage());
        }
    }

    // 2. CONFIRMAR PAGO
    // 2. CONFIRMAR PAGO
    @Transactional
    public void confirmPayment(String userEmail, String sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        try {
            Session session = Session.retrieve(sessionId);

            // 🛑 1. VERIFICACIÓN DE PROPIEDAD (SEGURIDAD CRÍTICA)
            // Comprobamos que el usuario que inició el pago es el mismo que lo confirma
            String clientRef = session.getClientReferenceId();

            if (clientRef == null || !clientRef.equals(user.getId().toString())) {
                // Logueamos esto como advertencia de seguridad
                System.err.println(
                        "🚨 SEGURIDAD: Usuario " + user.getId() + " intentó apropiarse de la sesión " + sessionId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta sesión de pago no te pertenece.");
            }

            // 🛑 2. VERIFICACIÓN DE ESTADO
            if ("paid".equals(session.getPaymentStatus())) {
                String courseIdStr = session.getMetadata().get("course_id");

                Course course = courseRepository.findById(UUID.fromString(courseIdStr))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Curso no encontrado (Metadata corrupta)"));

                // Delegamos la matrícula
                enrollmentService.enrollUser(user, course, session.getPaymentIntent());

            } else {
                // Puede ser 'unpaid' o 'no_payment_required'
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago no se ha completado o ha fallado.");
            }

        } catch (ResponseStatusException e) {
            throw e; // Relanzamos nuestras propias excepciones
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error verificando pago: " + e.getMessage());
        }
    }
}