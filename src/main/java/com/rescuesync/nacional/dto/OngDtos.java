package com.rescuesync.nacional.dto;

import com.rescuesync.nacional.domain.NivelRiesgo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public final class OngDtos {
    private OngDtos() {
    }

    public record CertificacionResponse(Long id, String codigo, String nombre, String tipoRecurso, int nivel,
                                        String nivelNombre, NivelRiesgo riesgoMaximo) {
    }

    public record CertificacionOngResponse(String codigo, String nombre, String tipoRecurso, int nivel,
                                           NivelRiesgo riesgoMaximo, LocalDate vigenteHasta, boolean vigente) {
    }

    public record OngResponse(Long id, String nombre, String cuit, String email, String aliasLocal,
                              boolean habilitada, List<CertificacionOngResponse> certificaciones) {
    }

    public record HabilitacionRequest(@NotNull Boolean habilitada) {
    }

    public record AsignarCertificacionRequest(@NotBlank String codigoCertificacion, LocalDate vigenteHasta) {
    }

    public record ItemInventario(@NotBlank String tipoRecurso, @NotNull @Min(0) Integer cantidadTotal) {
    }

    public record InventarioRequest(@NotEmpty List<@Valid ItemInventario> items) {
    }

    public record RecursoOngResponse(String tipoRecurso, int cantidadTotal, int cantidadBloqueada,
                                     int cantidadDisponible) {
    }
}
