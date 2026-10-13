package com.rescuesync.nacional.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "compromiso_item")
public class CompromisoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "compromiso_id", nullable = false)
    private Compromiso compromiso;

    @Column(nullable = false, length = 254)
    private String tipoRecurso;

    @Column(name = "tipo_recurso_key", nullable = false, length = 254)
    private String tipoRecursoKey;

    @Column(nullable = false)
    private int cantidad;

    public CompromisoItem() {
    }

    public CompromisoItem(String tipoRecurso, String tipoRecursoKey, int cantidad) {
        this.tipoRecurso = tipoRecurso;
        this.tipoRecursoKey = tipoRecursoKey;
        this.cantidad = cantidad;
    }

    public Long getId() { return id; }
    public Compromiso getCompromiso() { return compromiso; }
    public void setCompromiso(Compromiso compromiso) { this.compromiso = compromiso; }
    public String getTipoRecurso() { return tipoRecurso; }
    public String getTipoRecursoKey() { return tipoRecursoKey; }
    public int getCantidad() { return cantidad; }
}
