package com.radioacademy.backend.service.payment;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.security.CustomUserDetails;
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
    private final EnrollmentService enrollmentService; 

    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    
    public String createCheckoutSession(CustomUserDetails userDetails, UUID courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        
        if (enrollmentRepository.existsByUserIdAndCourseId(userDetails.getId(), courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya tienes este curso.");
        }

        try {
            long amountInCents = course.getPrice().multiply(new BigDecimal(100)).longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(userDetails.getId().toString())
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
            
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error con Stripe: " + e.getMessage());
        }
    }

    
    
    @Transactional
    public void confirmPayment(CustomUserDetails userDetails, String sessionId) {

        try {
            Session session = Session.retrieve(sessionId);

            
            
            String clientRef = session.getClientReferenceId();

            if (clientRef == null || !clientRef.equals(userDetails.getId().toString())) {
                
                System.err.println(
                        "🚨 SEGURIDAD: Usuario " + userDetails.getId() + " intentó apropiarse de la sesión "
                                + sessionId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta sesión de pago no te pertenece.");
            }

            
            if ("paid".equals(session.getPaymentStatus())) {
                UUID courseId = UUID.fromString(session.getMetadata().get("course_id"));
                enrollmentService.enrollUser(userDetails.getId(), courseId, session.getPaymentIntent());
            } else {
                
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago no se ha completado o ha fallado.");
            }

        } catch (ResponseStatusException e) {
            throw e; 
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error verificando pago: " + e.getMessage());
        }
    }
}