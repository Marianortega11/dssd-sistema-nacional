package com.rescuesync.nacional.domain;

import com.rescuesync.nacional.util.Texto;
import jakarta.persistence.*;

/** Inventario de un tipo de recurso de una ONG. Disponible = total - bloqueada. */
@Entity
@Table(name = "recurso_ong",
        uniqueConstraints = @UniqueConstraint(name = "uk_recurso_ong_tipo", columnNames = {"ong_id", "tipo_recurso_key"}))
public class RecursoOng {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ong_id", nullable = false)
    private Ong ong;

    @Column(nullable = false, length = 254)
    private String tipoRecurso;

    @Column(name = "tipo_recurso_key", nullable = false, length = 254)
    private String tipoRecursoKey;

    @Column(nullable = false)
    private int cantidadTotal;

    @Column(nullable = false)
    private int cantidadBloqueada = 0;

    public RecursoOng() {
    }

    public RecursoOng(Ong ong, String tipoRecurso, int cantidadTotal) {
        this.ong = ong;
        setTipoRecurso(tipoRecurso);
        this.cantidadTotal = cantidadTotal;
    }

    public int disponible() {
        return cantidadTotal - cantidadBloqueada;
    }

    public Long getId() { return id; }
    public Ong getOng() { return ong; }
    public String getTipoRecurso() { return tipoRecurso; }
    public void setTipoRecurso(String tipoRecurso) {
        this.tipoRecurso = tipoRecurso.trim();
        this.tipoRecursoKey = Texto.clave(tipoRecurso);
    }
    public String getTipoRecursoKey() { return tipoRecursoKey; }
    public int getCantidadTotal() { return cantidadTotal; }
    public void setCantidadTotal(int cantidadTotal) { this.cantidadTotal = cantidadTotal; }
    public int getCantidadBloqueada() { return cantidadBloqueada; }
    public void setCantidadBloqueada(int cantidadBloqueada) { this.cantidadBloqueada = cantidadBloqueada; }
}
