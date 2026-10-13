package com.rescuesync.nacional.service;

import com.rescuesync.nacional.domain.*;
import com.rescuesync.nacional.dto.RecursoDtos.CierreProyectoResponse;
import com.rescuesync.nacional.dto.RecursoDtos.CompromisoResponse;
import com.rescuesync.nacional.exception.ApiException;
import com.rescuesync.nacional.repository.CompromisoRepository;
import com.rescuesync.nacional.repository.OngRepository;
import com.rescuesync.nacional.repository.RecursoOngRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Liberacion de recursos: finalizacion de una ONG y cierre del proyecto. Ambas operaciones son idempotentes. */
@Service
public class ProyectoService {

    private final OngService ongService;
    private final OngRepository ongs;
    private final RecursoOngRepository recursos;
    private final CompromisoRepository compromisos;
    private final TransactionTemplate tx;

    public ProyectoService(OngService ongService, OngRepository ongs, RecursoOngRepository recursos,
                           CompromisoRepository compromisos, PlatformTransactionManager tm) {
        this.ongService = ongService;
        this.ongs = ongs;
        this.recursos = recursos;
        this.compromisos = compromisos;
        this.tx = new TransactionTemplate(tm);
    }

    public List<CompromisoResponse> listar(String proyectoRef) {
        return compromisos.findByProyectoRefOrderByOngId(proyectoRef.trim()).stream()
                .map(RecursoService::aDto).toList();
    }

    /** La ONG reporta que finalizo sus actividades: se liberan sus recursos de ese proyecto. */
    public CompromisoResponse finalizarOng(String proyectoRef, String ongIdentificador) {
        final String ref = proyectoRef.trim();
        final Long ongId = ongService.resolver(ongIdentificador).getId();
        return tx.execute(st -> {
            ongs.lockById(ongId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ONG inexistente"));
            Compromiso c = compromisos.findByProyectoRefAndOngId(ref, ongId).orElseThrow(() -> new ApiException(
                    HttpStatus.NOT_FOUND, "La ONG no tiene compromiso en el proyecto " + ref));
            liberar(c);
            return RecursoService.aDto(c);
        });
    }

    /** Cierre del proyecto: libera todos los compromisos activos, en orden de ONG para evitar deadlocks. */
    public CierreProyectoResponse cerrar(String proyectoRef) {
        final String ref = proyectoRef.trim();
        List<Long> ongIds = compromisos.findByProyectoRefOrderByOngId(ref).stream()
                .map(c -> c.getOng().getId()).distinct().sorted().toList();
        if (ongIds.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No hay compromisos registrados para el proyecto " + ref);
        }
        return tx.execute(st -> {
            List<CompromisoResponse> resultado = new ArrayList<>();
            int liberados = 0;
            for (Long ongId : ongIds) {
                ongs.lockById(ongId);
                Compromiso c = compromisos.findByProyectoRefAndOngId(ref, ongId).orElseThrow();
                if (liberar(c)) liberados++;
                resultado.add(RecursoService.aDto(c));
            }
            return new CierreProyectoResponse(ref, "CERRADO", liberados, ongIds.size() - liberados, resultado);
        });
    }

    /** Debe invocarse con la fila de la ONG ya bloqueada. @return true si libero recursos en esta llamada. */
    private boolean liberar(Compromiso c) {
        if (c.getEstado() == EstadoCompromiso.LIBERADO) return false;
        Map<String, RecursoOng> filas = new HashMap<>();
        recursos.lockByOngAndKeys(c.getOng().getId(), c.getItems().stream().map(CompromisoItem::getTipoRecursoKey).toList())
                .forEach(r -> filas.put(r.getTipoRecursoKey(), r));
        for (CompromisoItem i : c.getItems()) {
            RecursoOng r = filas.get(i.getTipoRecursoKey());
            if (r != null) {
                r.setCantidadBloqueada(Math.max(0, r.getCantidadBloqueada() - i.getCantidad()));
            }
        }
        c.setEstado(EstadoCompromiso.LIBERADO);
        c.setFechaLiberacion(Instant.now());
        return true;
    }
}
