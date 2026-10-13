package com.rescuesync.nacional.controller;

import com.rescuesync.nacional.dto.RecursoDtos.*;
import com.rescuesync.nacional.service.RecursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recursos")
public class RecursoController {

    private final RecursoService recursos;

    public RecursoController(RecursoService recursos) {
        this.recursos = recursos;
    }

    @Tag(name = "2. Catalogo y consulta")
    @Operation(summary = "Disponibilidad global de recursos", description = "Suma del inventario de las ONGs habilitadas.")
    @GetMapping("/disponibilidad")
    public List<DisponibilidadResponse> disponibilidad(@RequestParam(required = false) String tipoRecurso) {
        return recursos.disponibilidadGlobal(tipoRecurso);
    }

    @Tag(name = "4. Concurrencia y bloqueo")
    @Operation(summary = "Validar restricciones (sin bloquear)",
            description = "Indica si el bloqueo seria viable y cuanto falta por recurso.")
    @PreAuthorize("hasAnyRole('SISTEMA','ADMIN')")
    @PostMapping("/validar-restricciones")
    public RestriccionesResponse validar(@Valid @RequestBody SolicitudRecursos req) {
        return recursos.validarRestricciones(req);
    }

    @Tag(name = "4. Concurrencia y bloqueo")
    @Operation(summary = "Bloquear / comprometer recursos al adjudicar",
            description = "Transaccional con SELECT FOR UPDATE en PostgreSQL. 201 si crea el compromiso, 200 si ya "
                    + "existia uno activo para (proyectoRef, ONG) (idempotente), 409 si los recursos no alcanzan.")
    @PreAuthorize("hasAnyRole('SISTEMA','ADMIN')")
    @PostMapping("/bloqueos")
    public ResponseEntity<CompromisoResponse> bloquear(@Valid @RequestBody SolicitudRecursos req) {
        var r = recursos.bloquear(req);
        return ResponseEntity.status(r.creado() ? HttpStatus.CREATED : HttpStatus.OK).body(r.compromiso());
    }
}
