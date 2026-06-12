package dev.pimon.newsletter.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.newsletter.config.NewsletterProperties;
import dev.pimon.newsletter.dto.SubscribeRequest;
import dev.pimon.newsletter.dto.SubscriptionResultDto;
import dev.pimon.newsletter.service.NewsletterSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class PublicNewsletterController {

    private final NewsletterSubscriptionService service;
    private final NewsletterProperties          props;

    /** Da de alta (o reactiva) un email y envía un correo de confirmación (doble opt-in). */
    @PostMapping("/subscribe")
    public ApiResponse<SubscriptionResultDto> subscribe(@Valid @RequestBody SubscribeRequest req) {
        SubscriptionResultDto result = service.subscribe(req);
        String message = result.alreadySubscribed()
                ? "Ya estabas suscrito a la newsletter."
                : "¡Casi listo! Revisa tu email para confirmar la suscripción.";
        return ApiResponse.ok(result, message);
    }

    /** Enlace de confirmación recibido por email. Confirma y redirige al frontend (o a errorRedirectUrl si el token no es válido). */
    @GetMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestParam String token) {
        String redirectUrl;
        try {
            service.confirm(token);
            redirectUrl = props.getConfirmRedirectUrl();
        } catch (AppException e) {
            redirectUrl = props.getErrorRedirectUrl();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    /** Enlace de baja recibido por email. Da de baja y redirige al frontend (o a errorRedirectUrl si el token no es válido). */
    @GetMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam String token) {
        String redirectUrl;
        try {
            service.unsubscribe(token);
            redirectUrl = props.getUnsubscribeRedirectUrl();
        } catch (AppException e) {
            redirectUrl = props.getErrorRedirectUrl();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
