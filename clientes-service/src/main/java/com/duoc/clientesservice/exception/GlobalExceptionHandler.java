package com.duoc.clientesservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteNoEncontradoException.class)
    ResponseEntity<ApiError> noEncontrado(ClienteNoEncontradoException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiError> solicitudInvalida(Exception exception, HttpServletRequest request) {
        String mensaje = exception instanceof MethodArgumentNotValidException validacion
                ? validacion.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .findFirst().orElse("Solicitud invalida")
                : exception.getMessage();
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, request.getRequestURI());
    }

    private ResponseEntity<ApiError> respuesta(HttpStatus status, String mensaje, String ruta) {
        return ResponseEntity.status(status).body(new ApiError(
                OffsetDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, ruta));
    }
}
