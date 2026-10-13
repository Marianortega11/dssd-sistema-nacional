package com.rescuesync.nacional.dto;

import com.rescuesync.nacional.domain.EstadoCompromiso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class RecursoDtos {
    private RecursoDtos() {
    }

    public record ItemRecurso(@NotBlank String tipoRecurso, @NotNull @Min(1) Integer cantidad) {
    }

    @Schema(description = "Recursos que una ONG compromete para un proyecto")
    public record SolicitudRecursos(
            @Schema(description = "Id de la emergencia / proyecto en el sistema local", example = "42")
            @NotBlank String proyectoRef,
            @Schema(description = "Alias local (username en Bonita) o CUIT de la ONG", example = "april.sanchez")
            @NotBlank String ong,
            @NotEmpty List<@Valid ItemRecurso> items) {
    }

    public record DisponibilidadResponse(String tipoRecurso, long cantidadTotal, long cantidadBloqueada,
                                         long cantidadDisponible, long ongsAportantes) {
    }

    public record ItemRestriccion(String tipoRecurso, int solicitada, int disponible, boolean suficiente,
                                  int faltante) {
    }

    public record RestriccionesResponse(Long ongId, String ongNombre, String proyectoRef, boolean viable,
                                        List<ItemRestriccion> items, List<String> motivos) {
    }

    public record ItemCompromiso(String tipoRecurso, int cantidad) {
    }

    public record CompromisoResponse(Long id, String proyectoRef, Long ongId, String ongNombre,
                                     EstadoCompromiso estado, List<ItemCompromiso> items, Instant fechaCreacion,
                                     Instant fechaLiberacion) {
    }

    public record FinalizacionOngRequest(@NotBlank String ong) {
    }

    public record CierreProyectoResponse(String proyectoRef, String estado, int compromisosLiberados,
                                         int compromisosYaLiberados, List<CompromisoResponse> compromisos) {
    }
}
