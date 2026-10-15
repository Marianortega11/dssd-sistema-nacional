package com.rescuesync.nacional.domain;

import com.rescuesync.nacional.util.Texto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** Inventario de un tipo de recurso de una ONG. Disponible = total - bloqueada. */
@Entity
@Table(name = "recurso_ong",
        uniqueConstraints = @UniqueConstraint(name = "uk_recurso_ong_tipo", columnNames = {"ong_id", "tipo_recurso_key"}))
@Getter
@Setter
public class RecursoOng {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ong_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Ong ong;

    @Column(nullable = false, length = 254)
    private String tipoRecurso;

    /** Derivada de tipoRecurso (sin tildes, minusculas); solo se asigna desde setTipoRecurso. */
    @Column(name = "tipo_recurso_key", nullable = false, length = 254)
    @Setter(AccessLevel.NONE)
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

    public void setTipoRecurso(String tipoRecurso) {
        this.tipoRecurso = tipoRecurso.trim();
        this.tipoRecursoKey = Texto.clave(tipoRecurso);
    }
}
