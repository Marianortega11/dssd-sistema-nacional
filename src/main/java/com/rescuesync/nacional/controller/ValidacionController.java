package com.rescuesync.nacional.controller;

import com.rescuesync.nacional.dto.ValidacionDtos.*;
import com.rescuesync.nacional.service.CompetenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/validacion")
@Tag(name = "3. Validacion por niveles de competencia")
public class ValidacionController {

    private final CompetenciaService competencias;

    public ValidacionController(CompetenciaService competencias) {
        this.competencias = competencias;
    }

    @Operation(summary = "Evaluar el perfil de competencia de una ONG para una oferta",
            description = "No rechaza: devuelve por cada recurso el nivel, el riesgo maximo permitido y el rol "
                    + "(PRINCIPAL / APOYO / NO_HABILITADA) con el que la ONG puede cubrir el lote.")
    @PreAuthorize("hasAnyRole('SISTEMA','ADMIN')")
    @PostMapping("/competencias")
    public PerfilCompetencia evaluar(@Valid @RequestBody EvaluacionRequest req) {
        return competencias.evaluar(req);
    }
}
