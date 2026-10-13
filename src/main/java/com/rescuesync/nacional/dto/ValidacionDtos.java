package com.rescuesync.nacional.dto;

import com.rescuesync.nacional.domain.NivelRiesgo;
import io.swagger.v3.oas.annotations.media.Schema;
import com.rescuesync.nacional.dto.RecursoDtos.ItemRecurso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class ValidacionDtos {
    private ValidacionDtos() {
    }

    public record EvaluacionRequest(
            @Schema(description = "Alias local (username en Bonita) o CUIT de la ONG", example = "april.sanchez")
            @NotBlank String ong,
            @Schema(description = "Gravedad de la emergencia: BAJO, MEDIO, ALTO o CRITICO", example = "ALTO")
            @NotBlank String gravedad,
            @NotEmpty List<@Valid ItemRecurso> items) {
    }

    public enum RolPermitido {
        /** Puede asumir el lote como responsable principal. */
        PRINCIPAL,
        /** Puede aportar como apoyo secundario (certificacion insuficiente para el riesgo, o sin certificacion). */
        APOYO,
        /** La ONG aun no esta habilitada en el Sistema Nacional. */
        NO_HABILITADA
    }

    public enum Resultado {
        HABILITADA_PRINCIPAL, HABILITACION_PARCIAL, HABILITADA_APOYO, NO_HABILITADA
    }

    public record CapacidadHabilitada(String tipoRecurso, int cantidadSolicitada, int cantidadDisponible,
                                      boolean cubreCantidad, int nivel, String nivelNombre,
                                      NivelRiesgo riesgoMaximoPermitido, RolPermitido rolPermitido,
                                      String certificacion, String motivo) {
    }

    public record PerfilCompetencia(Long ongId, String ongNombre, NivelRiesgo gravedadEmergencia,
                                    Resultado resultado, int nivelHabilitacion, NivelRiesgo riesgoMaximoPermitido,
                                    List<CapacidadHabilitada> capacidades, List<String> recomendaciones) {
    }
}
