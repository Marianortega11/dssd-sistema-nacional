package com.rescuesync.nacional.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Bloqueo de recursos de una ONG para un proyecto (emergencia) adjudicado. */
@Entity
@Table(name = "compromiso",
        uniqueConstraints = @UniqueConstraint(name = "uk_compromiso_proyecto_ong", columnNames = {"proyecto_ref", "ong_id"}))
@Getter
@Setter
public class Compromiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /** Referencia al proyecto/emergencia en el sistema local (ej. id de emergencia o de caso Bonita). */
    @Column(name = "proyecto_ref", nullable = false, length = 100)
    private String proyectoRef;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ong_id", nullable = false)
    private Ong ong;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCompromiso estado = EstadoCompromiso.ACTIVO;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant fechaCreacion = Instant.now();

    private Instant fechaLiberacion;

    @OneToMany(mappedBy = "compromiso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Setter(AccessLevel.NONE)
    private List<CompromisoItem> items = new ArrayList<>();

    public void agregarItem(CompromisoItem item) {
        item.setCompromiso(this);
        items.add(item);
    }
}
