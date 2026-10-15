package com.rescuesync.nacional.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, String rol, Long ongId) {
    }

    public record RegistroOngRequest(
            @NotBlank @Size(max = 200) String nombre,
            @NotBlank @Pattern(regexp = "^[0-9]{2}-?[0-9]{8}-?[0-9]$", message = "CUIT invalido (formato 20-12345678-9)") String cuit,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 3, max = 100) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 100) String aliasLocal) {
    }

    public record RegistroOngResponse(Long ongId, String nombre, String cuit, boolean habilitada, String username,
                                      String mensaje) {
    }
}
