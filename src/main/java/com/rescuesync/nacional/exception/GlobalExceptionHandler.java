package com.rescuesync.nacional.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException ex) {
        return body(ex.getStatus(), ex.getMessage(), ex.getDetalles());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return body(HttpStatus.BAD_REQUEST, "Solicitud invalida", errores);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String, Object>> malFormada(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, ex instanceof IllegalArgumentException ? ex.getMessage()
                : "Cuerpo de la solicitud ilegible o mal formado", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> denegado(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "No tiene permisos para esta operacion", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad", ex);
        return body(HttpStatus.CONFLICT, "Conflicto con datos existentes (registro duplicado)", null);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<Map<String, Object>> lock(PessimisticLockingFailureException ex) {
        log.warn("Timeout o fallo de lock", ex);
        return body(HttpStatus.CONFLICT, "Los recursos estan siendo modificados por otra operacion; reintente", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> generico(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, Object detalles) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("status", status.value());
        m.put("error", status.getReasonPhrase());
        m.put("message", message);
        if (detalles != null) m.put("detalles", detalles);
        return ResponseEntity.status(status).body(m);
    }
}
