package com.rescuesync.nacional.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ong_certificacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_ong_certificacion", columnNames = {"ong_id", "certificacion_id"}))
public class OngCertificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ong_id", nullable = false)
    private Ong ong;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "certificacion_id", nullable = false)
    private Certificacion certificacion;

    /** null = sin vencimiento. */
    private LocalDate vigenteHasta;

    public OngCertificacion() {
    }

    public OngCertificacion(Ong ong, Certificacion certificacion, LocalDate vigenteHasta) {
        this.ong = ong;
        this.certificacion = certificacion;
        this.vigenteHasta = vigenteHasta;
    }

    public boolean vigente(LocalDate hoy) {
        return vigenteHasta == null || !vigenteHasta.isBefore(hoy);
    }

    public Long getId() { return id; }
    public Ong getOng() { return ong; }
    public Certificacion getCertificacion() { return certificacion; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public void setVigenteHasta(LocalDate vigenteHasta) { this.vigenteHasta = vigenteHasta; }
}
