package com.rescuesync.nacional.controller;

import com.rescuesync.nacional.dto.AuthDtos.*;
import com.rescuesync.nacional.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Autenticacion y cuentas")
@SecurityRequirements
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @Operation(summary = "Inicio de sesion", description = "Devuelve un JWT (Bearer) para usar en el resto de la API.")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @Operation(summary = "Registro / onboarding de ONG",
            description = "Crea la ONG (pendiente de habilitacion) y su cuenta de acceso con rol ONG.")
    @PostMapping("/register")
    public ResponseEntity<RegistroOngResponse> registrar(@Valid @RequestBody RegistroOngRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.registrar(req));
    }
}
