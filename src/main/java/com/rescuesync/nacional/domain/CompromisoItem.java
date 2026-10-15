package com.rescuesync.nacional.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "compromiso_item")
@Getter
@Setter
public class CompromisoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "compromiso_id", nullable = false)
    private Compromiso compromiso;

    @Column(nullable = false, length = 254)
    @Setter(AccessLevel.NONE)
    private String tipoRecurso;

    @Column(name = "tipo_recurso_key", nullable = false, length = 254)
    @Setter(AccessLevel.NONE)
    private String tipoRecursoKey;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private int cantidad;

    public CompromisoItem() {
    }

    public CompromisoItem(String tipoRecurso, String tipoRecursoKey, int cantidad) {
        this.tipoRecurso = tipoRecurso;
        this.tipoRecursoKey = tipoRecursoKey;
        this.cantidad = cantidad;
    }
}
