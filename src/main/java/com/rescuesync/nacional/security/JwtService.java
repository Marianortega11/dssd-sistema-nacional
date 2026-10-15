package com.rescuesync.nacional.security;

import com.rescuesync.nacional.domain.Cuenta;
import com.rescuesync.nacional.domain.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration duracion;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long minutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.duracion = Duration.ofMinutes(minutos);
    }

    public long segundosDeVida() {
        return duracion.toSeconds();
    }

    public String generar(Cuenta cuenta) {
        Instant ahora = Instant.now();
        var b = Jwts.builder()
                .subject(cuenta.getUsername().trim())
                .claim("rol", cuenta.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)));
        if (cuenta.getOng() != null) {
            b.claim("ongId", cuenta.getOng().getId());
        }
        return b.signWith(key).compact();
    }

    /** Devuelve vacio si el token es invalido, esta vencido o fue alterado. */
    public Optional<AuthPrincipal> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Number ongId = c.get("ongId", Number.class);
            return Optional.of(new AuthPrincipal(c.getSubject(), Rol.valueOf(c.get("rol", String.class)),
                    ongId == null ? null : ongId.longValue()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
