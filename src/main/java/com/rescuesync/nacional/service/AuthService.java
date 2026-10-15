package com.rescuesync.nacional.service;

import com.rescuesync.nacional.domain.Cuenta;
import com.rescuesync.nacional.domain.Ong;
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
    public LoginResponse login(LoginRequest req) {
        Cuenta c = cuentas.findByUsername(req.username().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
        return new LoginResponse(jwt.generar(c), "Bearer", jwt.segundosDeVida(), c.getRol().name(),
                c.getOng() == null ? null : c.getOng().getId());
    }

    /** Onboarding: crea la ONG (pendiente de habilitacion) y su cuenta de acceso. */
    @Transactional
    public RegistroOngResponse registrar(RegistroOngRequest req) {
        String username = req.username().trim();
        String cuit = Texto.soloDigitos(req.cuit());
        String alias = req.aliasLocal() == null || req.aliasLocal().isBlank() ? null : req.aliasLocal().trim();
        String email = req.email().trim();
        String nombre = req.nombre().trim();
        if (cuentas.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "El nombre de usuario ya esta en uso");
        }
        if (ongs.existsByCuit(cuit)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una ONG registrada con ese CUIT");
        }
        if (alias != null && ongs.existsByAliasLocalIgnoreCase(alias)) {
            throw new ApiException(HttpStatus.CONFLICT, "El alias local ya esta asociado a otra ONG");
        }

        Ong ong = new Ong(nombre,cuit,alias,email);
        Cuenta cuenta = new Cuenta(username,encoder.encode(req.password()), ong);

        ongs.save(ong);
        cuentas.save(cuenta);

        //este return es medio raro porque devuelve el ID pero para el swagger esta bueno
        return new RegistroOngResponse(ong.getId(), ong.getNombre(), ong.getCuit(), ong.isHabilitada(), username,
                "ONG registrada. Queda pendiente de habilitacion por el Sistema Nacional.");
    }
}
