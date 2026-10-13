package com.rescuesync.nacional.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Object detalles;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ApiException(HttpStatus status, String message, Object detalles) {
        super(message);
        this.status = status;
        this.detalles = detalles;
    }

    public HttpStatus getStatus() { return status; }
    public Object getDetalles() { return detalles; }
}
