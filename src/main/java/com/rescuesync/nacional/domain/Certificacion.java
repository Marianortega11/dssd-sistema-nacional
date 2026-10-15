package com.rescuesync.nacional.domain;

import com.rescuesync.nacional.util.Texto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Catalogo nacional de certificaciones. Cada una habilita un tipo de recurso con un nivel de
 * competencia (1 basico, 2 intermedio, 3 avanzado) y un riesgo maximo operable.
 */
@Entity
@Table(name = "certificacion")
@Getter
@Setter
public class Certificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 254)
    private String tipoRecurso;

    /** Derivada de tipoRecurso (sin tildes, minusculas); solo se asigna desde setTipoRecurso. */
    @Column(nullable = false, length = 254)
    @Setter(AccessLevel.NONE)
    private String tipoRecursoKey;

    @Column(nullable = false)
    private int nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelRiesgo riesgoMaximo;

    public Certificacion() {
    }

    public Certificacion(String codigo, String nombre, String tipoRecurso, int nivel, NivelRiesgo riesgoMaximo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.nivel = nivel;
        this.riesgoMaximo = riesgoMaximo;
        setTipoRecurso(tipoRecurso);
    }

    public static String nombreNivel(int nivel) {
        return switch (nivel) {
            case 1 -> "BASICO";
            case 2 -> "INTERMEDIO";
            case 3 -> "AVANZADO";
            default -> "SIN_CERTIFICACION";
        };
    }

    public void setTipoRecurso(String tipoRecurso) {
        this.tipoRecurso = tipoRecurso;
        this.tipoRecursoKey = Texto.clave(tipoRecurso);
    }
}
