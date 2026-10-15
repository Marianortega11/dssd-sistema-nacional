package com.rescuesync.nacional.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "ong")
@NoArgsConstructor 
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

    public Ong(String nombre, String cuit, String alias, String email) {
        this.aliasLocal = alias;
        this.cuit = cuit;
        this.nombre = nombre;
        this.email = email;
        this.habilitada = false;
        
    }
    

}
