package com.rescuesync.nacional.config;

import com.rescuesync.nacional.domain.*;
import com.rescuesync.nacional.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Datos iniciales idempotentes: cuentas de servicio, catalogo de certificaciones y (opcional) ONGs de demo. */
@Component
public class DataSeeder implements ApplicationRunner {

    private final CuentaRepository cuentas;
    private final OngRepository ongs;
    private final CertificacionRepository certificaciones;
    private final OngCertificacionRepository ongCertificaciones;
    private final RecursoOngRepository recursos;
    private final PasswordEncoder encoder;

    @Value("${app.seed.enabled}") private boolean enabled;
    @Value("${app.seed.demo}") private boolean demo;
    @Value("${app.seed.admin-password}") private String adminPassword;
    @Value("${app.seed.sistema-password}") private String sistemaPassword;
    @Value("${app.seed.demo-password}") private String demoPassword;

    public DataSeeder(CuentaRepository cuentas, OngRepository ongs, CertificacionRepository certificaciones,
                      OngCertificacionRepository ongCertificaciones, RecursoOngRepository recursos,
                      PasswordEncoder encoder) {
        this.cuentas = cuentas;
        this.ongs = ongs;
        this.certificaciones = certificaciones;
        this.ongCertificaciones = ongCertificaciones;
        this.recursos = recursos;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        cuenta("admin", adminPassword, Rol.ADMIN, null);
        cuenta("bonita", sistemaPassword, Rol.SISTEMA, null);

        cert("PARAMEDICO-AVANZADO", "Paramedico avanzado", "Paramedicos", 3, NivelRiesgo.CRITICO);
        cert("PARAMEDICO-BASICO", "Paramedico basico", "Paramedicos", 1, NivelRiesgo.MEDIO);
        cert("RESCATE-ACUATICO", "Rescate acuatico", "Rescatistas", 3, NivelRiesgo.ALTO);
        cert("BRIGADISTA-FORESTAL", "Brigadista forestal", "Brigadistas", 3, NivelRiesgo.CRITICO);
        cert("LOGISTICA-ALIMENTARIA", "Logistica alimentaria", "Raciones de alimento", 2, NivelRiesgo.ALTO);
        cert("AGUA-POTABLE", "Provision de agua potable", "Agua Potable (Litros)", 2, NivelRiesgo.ALTO);

        if (demo) {
            Ong cruz = ong("Cruz Roja Argentina (demo)", "30500000000", "cruzroja@demo.org", "april.sanchez", "cruzroja");
            asignar(cruz, "PARAMEDICO-AVANZADO");
            asignar(cruz, "LOGISTICA-ALIMENTARIA");
            asignar(cruz, "AGUA-POTABLE");
            stock(cruz, "Paramedicos", 20);
            stock(cruz, "Raciones de alimento", 5000);
            stock(cruz, "Agua Potable (Litros)", 20000);

            Ong bomberos = ong("Bomberos Voluntarios (demo)", "30700000001", "bomberos@demo.org", "helen.kelly", "bomberos");
            asignar(bomberos, "BRIGADISTA-FORESTAL");
            asignar(bomberos, "PARAMEDICO-BASICO");
            stock(bomberos, "Brigadistas", 40);
            stock(bomberos, "Paramedicos", 8);
            stock(bomberos, "Agua Potable (Litros)", 8000);
        }
    }

    private void cuenta(String username, String password, Rol rol, Ong ong) {
        if (cuentas.existsByUsername(username)) return;
        Cuenta c = new Cuenta();
        c.setUsername(username);
        c.setPasswordHash(encoder.encode(password));
        c.setRol(rol);
        c.setOng(ong);
        cuentas.save(c);
    }

    private void cert(String codigo, String nombre, String tipo, int nivel, NivelRiesgo riesgo) {
        if (certificaciones.findByCodigo(codigo).isEmpty()) {
            certificaciones.save(new Certificacion(codigo, nombre, tipo, nivel, riesgo));
        }
    }

    private Ong ong(String nombre, String cuit, String email, String alias, String username) {
        Ong o = ongs.findByCuit(cuit).orElseGet(() -> {
            Ong n = new Ong();
            n.setNombre(nombre);
            n.setCuit(cuit);
            n.setEmail(email);
            n.setAliasLocal(alias);
            n.setHabilitada(true);
            return ongs.save(n);
        });
        cuenta(username, demoPassword, Rol.ONG, o);
        return o;
    }

    private void asignar(Ong ong, String codigo) {
        Certificacion c = certificaciones.findByCodigo(codigo).orElseThrow();
        if (ongCertificaciones.findByOngIdAndCertificacionId(ong.getId(), c.getId()).isEmpty()) {
            ongCertificaciones.save(new OngCertificacion(ong, c, null));
        }
    }

    private void stock(Ong ong, String tipo, int total) {
        String key = com.rescuesync.nacional.util.Texto.clave(tipo);
        if (recursos.findByOngIdAndTipoRecursoKey(ong.getId(), key).isEmpty()) {
            recursos.save(new RecursoOng(ong, tipo, total));
        }
    }
}
