package com.rescuesync.nacional.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ong")
public class Ong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    /** CUIT normalizado (solo 11 digitos). */
    @Column(nullable = false, unique = true, length = 11)
    private String cuit;

    @Column(nullable = false, length = 254)
    private String email;

    /** Username con el que la ONG opera en el sistema local / Bonita (permite identificarla desde Bonita). */
    @Column(unique = true, length = 100)
    private String aliasLocal;

    /** Una ONG recien registrada queda pendiente hasta que un ADMIN la habilita. */
    @Column(nullable = false)
    private boolean habilitada = false;

    @Column(nullable = false)
    private Instant fechaAlta = Instant.now();

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAliasLocal() { return aliasLocal; }
    public void setAliasLocal(String aliasLocal) { this.aliasLocal = aliasLocal; }
    public boolean isHabilitada() { return habilitada; }
    public void setHabilitada(boolean habilitada) { this.habilitada = habilitada; }
    public Instant getFechaAlta() { return fechaAlta; }
}
