package com.rescuesync.nacional.controller;

import com.rescuesync.nacional.dto.OngDtos.*;
import com.rescuesync.nacional.service.OngService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "2. Catalogo y consulta")
public class CatalogoController {

    private final OngService ongs;

    public CatalogoController(OngService ongs) {
        this.ongs = ongs;
    }

    @Operation(summary = "Catalogo de certificaciones")
    @GetMapping("/certificaciones")
    public List<CertificacionResponse> certificaciones() {
        return ongs.listarCertificaciones();
    }

    @Operation(summary = "ONGs registradas", description = "Filtrar con ?habilitada=true para ver solo las habilitadas.")
    @GetMapping("/ongs")
    public List<OngResponse> listar(@RequestParam(required = false) Boolean habilitada) {
        return ongs.listar(habilitada);
    }

    @Operation(summary = "Detalle de una ONG con sus certificaciones")
    @GetMapping("/ongs/{id}")
    public OngResponse detalle(@PathVariable Long id) {
        return ongs.detalle(id);
    }

    @Operation(summary = "Habilitar / deshabilitar una ONG (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/ongs/{id}/habilitacion")
    public OngResponse habilitar(@PathVariable Long id, @Valid @RequestBody HabilitacionRequest req) {
        return ongs.cambiarHabilitacion(id, req.habilitada());
    }

    @Operation(summary = "Asignar certificacion a una ONG (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ongs/{id}/certificaciones")
    public OngResponse asignarCertificacion(@PathVariable Long id, @Valid @RequestBody AsignarCertificacionRequest req) {
        return ongs.asignarCertificacion(id, req.codigoCertificacion(), req.vigenteHasta());
    }

    @Operation(summary = "Inventario de recursos de una ONG")
    @GetMapping("/ongs/{id}/recursos")
    public List<RecursoOngResponse> inventario(@PathVariable Long id) {
        return ongs.inventario(id);
    }

    @Operation(summary = "Cargar / actualizar el inventario de una ONG (ADMIN o la propia ONG)")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ONG') and #id == principal.ongId)")
    @PutMapping("/ongs/{id}/recursos")
    public List<RecursoOngResponse> actualizarInventario(@PathVariable Long id, @Valid @RequestBody InventarioRequest req) {
        return ongs.actualizarInventario(id, req.items());
    }
}
