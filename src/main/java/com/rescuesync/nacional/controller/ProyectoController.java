package com.rescuesync.nacional.controller;

import com.rescuesync.nacional.dto.RecursoDtos.*;
import com.rescuesync.nacional.service.ProyectoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proyectos")
@Tag(name = "5. Liberacion y cierre")
public class ProyectoController {

    private final ProyectoService proyectos;

    public ProyectoController(ProyectoService proyectos) {
        this.proyectos = proyectos;
    }

    @Operation(summary = "Compromisos de un proyecto")
    @GetMapping("/{proyectoRef}/compromisos")
    public List<CompromisoResponse> compromisos(@PathVariable String proyectoRef) {
        return proyectos.listar(proyectoRef);
    }

    @Operation(summary = "Reportar finalizacion de las actividades de una ONG",
            description = "Libera los recursos que esa ONG tenia bloqueados para el proyecto. Idempotente.")
    @PreAuthorize("hasAnyRole('SISTEMA','ADMIN')")
    @PostMapping("/{proyectoRef}/finalizacion")
    public CompromisoResponse finalizar(@PathVariable String proyectoRef, @Valid @RequestBody FinalizacionOngRequest req) {
        return proyectos.finalizarOng(proyectoRef, req.ong());
    }

    @Operation(summary = "Cerrar el proyecto y liberar todos los recursos bloqueados",
            description = "Libera a nivel nacional todos los compromisos activos del proyecto. Idempotente.")
    @PreAuthorize("hasAnyRole('SISTEMA','ADMIN')")
    @PostMapping("/{proyectoRef}/cierre")
    public CierreProyectoResponse cerrar(@PathVariable String proyectoRef) {
        return proyectos.cerrar(proyectoRef);
    }
}
