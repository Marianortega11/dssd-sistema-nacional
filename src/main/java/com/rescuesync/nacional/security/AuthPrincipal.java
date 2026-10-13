package com.rescuesync.nacional.security;

import com.rescuesync.nacional.domain.Rol;

/** Identidad autenticada extraida del JWT (clase con getters para poder usarla en expresiones SpEL). */
public class AuthPrincipal {
    private final String username;
    private final Rol rol;
    private final Long ongId;

    public AuthPrincipal(String username, Rol rol, Long ongId) {
        this.username = username;
        this.rol = rol;
        this.ongId = ongId;
    }

    public String getUsername() { return username; }
    public Rol getRol() { return rol; }
    public Long getOngId() { return ongId; }
}
