package com.rescuesync.nacional.service;

import com.rescuesync.nacional.domain.Cuenta;
import com.rescuesync.nacional.domain.Ong;
import com.rescuesync.nacional.domain.Rol;
import com.rescuesync.nacional.dto.AuthDtos.*;
import com.rescuesync.nacional.exception.ApiException;
import com.rescuesync.nacional.repository.CuentaRepository;
import com.rescuesync.nacional.repository.OngRepository;
import com.rescuesync.nacional.security.JwtService;
import com.rescuesync.nacional.util.Texto;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CuentaRepository cuentas;
    private final OngRepository ongs;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(CuentaRepository cuentas, OngRepository ongs, PasswordEncoder encoder, JwtService jwt) {
        this.cuentas = cuentas;
        this.ongs = ongs;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Cuenta c = cuentas.findByUsername(req.username().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
        return new TokenResponse(jwt.generar(c), "Bearer", jwt.segundosDeVida(), c.getRol().name(),
                c.getOng() == null ? null : c.getOng().getId());
    }

    /** Onboarding: crea la ONG (pendiente de habilitacion) y su cuenta de acceso. */
    @Transactional
    public RegistroOngResponse registrar(RegistroOngRequest req) {
        String username = req.username().trim();
        String cuit = Texto.soloDigitos(req.cuit());
        String alias = req.aliasLocal() == null || req.aliasLocal().isBlank() ? null : req.aliasLocal().trim();

        if (cuentas.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "El nombre de usuario ya esta en uso");
        }
        if (ongs.existsByCuit(cuit)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una ONG registrada con ese CUIT");
        }
        if (alias != null && ongs.existsByAliasLocalIgnoreCase(alias)) {
            throw new ApiException(HttpStatus.CONFLICT, "El alias local ya esta asociado a otra ONG");
        }

        Ong ong = new Ong();
        ong.setNombre(req.nombre().trim());
        ong.setCuit(cuit);
        ong.setEmail(req.email().trim());
        ong.setAliasLocal(alias);
        ong.setHabilitada(false);
        ong = ongs.save(ong);

        Cuenta cuenta = new Cuenta();
        cuenta.setUsername(username);
        cuenta.setPasswordHash(encoder.encode(req.password()));
        cuenta.setRol(Rol.ONG);
        cuenta.setOng(ong);
        cuentas.save(cuenta);

        return new RegistroOngResponse(ong.getId(), ong.getNombre(), ong.getCuit(), false, username,
                "ONG registrada. Queda pendiente de habilitacion por el Sistema Nacional.");
    }
}
