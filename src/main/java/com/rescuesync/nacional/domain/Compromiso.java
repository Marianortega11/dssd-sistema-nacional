package com.rescuesync.nacional.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Bloqueo de recursos de una ONG para un proyecto (emergencia) adjudicado. */
@Entity
@Table(name = "compromiso",
        uniqueConstraints = @UniqueConstraint(name = "uk_compromiso_proyecto_ong", columnNames = {"proyecto_ref", "ong_id"}))
public class Compromiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Instant fechaCreacion = Instant.now();

    private Instant fechaLiberacion;

    @OneToMany(mappedBy = "compromiso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CompromisoItem> items = new ArrayList<>();

    public void agregarItem(CompromisoItem item) {
        item.setCompromiso(this);
        items.add(item);
    }

    public Long getId() { return id; }
    public String getProyectoRef() { return proyectoRef; }
    public void setProyectoRef(String proyectoRef) { this.proyectoRef = proyectoRef; }
    public Ong getOng() { return ong; }
    public void setOng(Ong ong) { this.ong = ong; }
    public EstadoCompromiso getEstado() { return estado; }
    public void setEstado(EstadoCompromiso estado) { this.estado = estado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaLiberacion() { return fechaLiberacion; }
    public void setFechaLiberacion(Instant fechaLiberacion) { this.fechaLiberacion = fechaLiberacion; }
    public List<CompromisoItem> getItems() { return items; }
}
