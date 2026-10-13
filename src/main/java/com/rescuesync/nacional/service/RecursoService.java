package com.rescuesync.nacional.service;

import com.rescuesync.nacional.domain.*;
import com.rescuesync.nacional.dto.RecursoDtos.*;
import com.rescuesync.nacional.exception.ApiException;
import com.rescuesync.nacional.repository.CompromisoRepository;
import com.rescuesync.nacional.repository.OngRepository;
import com.rescuesync.nacional.repository.RecursoOngRepository;
import com.rescuesync.nacional.util.Texto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

@Service
public class RecursoService {

    public record ResultadoBloqueo(CompromisoResponse compromiso, boolean creado) {
    }

    private final OngService ongService;
    private final OngRepository ongs;
    private final RecursoOngRepository recursos;
    private final CompromisoRepository compromisos;
    private final TransactionTemplate tx;

    public RecursoService(OngService ongService, OngRepository ongs, RecursoOngRepository recursos,
                          CompromisoRepository compromisos, PlatformTransactionManager tm) {
        this.ongService = ongService;
        this.ongs = ongs;
        this.recursos = recursos;
        this.compromisos = compromisos;
        this.tx = new TransactionTemplate(tm);
    }

    /** Disponibilidad global (ONGs habilitadas), opcionalmente de un solo tipo de recurso. */
    public List<DisponibilidadResponse> disponibilidadGlobal(String tipoRecurso) {
        List<Object[]> filas = tipoRecurso == null || tipoRecurso.isBlank()
                ? recursos.disponibilidadGlobal()
                : recursos.disponibilidadGlobalPorTipo(Texto.clave(tipoRecurso));
        return filas.stream().map(f -> {
            long total = ((Number) f[2]).longValue();
            long bloq = ((Number) f[3]).longValue();
            return new DisponibilidadResponse((String) f[1], total, bloq, total - bloq, ((Number) f[4]).longValue());
        }).toList();
    }

    /** Validacion de restricciones sin bloquear (consulta). */
    public RestriccionesResponse validarRestricciones(SolicitudRecursos s) {
        Ong ong = ongService.resolver(s.ong());
        Map<String, ItemRecurso> items = agrupar(s.items());
        Map<String, RecursoOng> inventario = new HashMap<>();
        recursos.findByOngIdOrderByTipoRecurso(ong.getId()).forEach(r -> inventario.put(r.getTipoRecursoKey(), r));

        List<String> motivos = new ArrayList<>();
        if (!ong.isHabilitada()) motivos.add("La ONG no esta habilitada en el Sistema Nacional");
        var existente = compromisos.findByProyectoRefAndOngId(s.proyectoRef().trim(), ong.getId());
        boolean yaActivo = existente.isPresent() && existente.get().getEstado() == EstadoCompromiso.ACTIVO;
        if (existente.isPresent() && !yaActivo) motivos.add("El compromiso de este proyecto ya fue liberado");
        if (yaActivo) motivos.add("Ya existe un compromiso activo para este proyecto (el bloqueo es idempotente)");

        List<ItemRestriccion> detalle = new ArrayList<>();
        for (var e : items.entrySet()) {
            RecursoOng r = inventario.get(e.getKey());
            int disp = r == null ? 0 : r.disponible();
            int pedida = e.getValue().cantidad();
            boolean ok = disp >= pedida;
            detalle.add(new ItemRestriccion(e.getValue().tipoRecurso(), pedida, disp, ok, ok ? 0 : pedida - disp));
            if (!ok && !yaActivo) {
                motivos.add("Disponible insuficiente de '" + e.getValue().tipoRecurso() + "'");
            }
        }
        boolean viable = yaActivo || (ong.isHabilitada() && existente.isEmpty()
                && detalle.stream().allMatch(ItemRestriccion::suficiente));
        return new RestriccionesResponse(ong.getId(), ong.getNombre(), s.proyectoRef().trim(), viable, detalle, motivos);
    }

    /**
     * Bloqueo transaccional. Serializa por ONG con SELECT ... FOR UPDATE sobre la fila de la ONG y luego sobre sus
     * filas de inventario (orden estable), de modo que dos adjudicaciones simultaneas nunca sobre-asignan.
     * Es idempotente por (proyectoRef, ONG): reintentos de Bonita devuelven el compromiso activo existente.
     */
    public ResultadoBloqueo bloquear(SolicitudRecursos s) {
        final String ref = s.proyectoRef().trim();
        final Long ongId = ongService.resolver(s.ong()).getId();
        final Map<String, ItemRecurso> items = agrupar(s.items());

        return tx.execute(st -> {
            Ong ong = ongs.lockById(ongId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ONG inexistente"));
            if (!ong.isHabilitada()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La ONG no esta habilitada en el Sistema Nacional");
            }
            var existente = compromisos.findByProyectoRefAndOngId(ref, ongId);
            if (existente.isPresent()) {
                if (existente.get().getEstado() == EstadoCompromiso.ACTIVO) {
                    return new ResultadoBloqueo(aDto(existente.get()), false);
                }
                throw new ApiException(HttpStatus.CONFLICT, "El compromiso de este proyecto ya fue liberado");
            }

            Map<String, RecursoOng> filas = new HashMap<>();
            recursos.lockByOngAndKeys(ongId, items.keySet()).forEach(r -> filas.put(r.getTipoRecursoKey(), r));

            List<ItemRestriccion> faltantes = new ArrayList<>();
            for (var e : items.entrySet()) {
                RecursoOng r = filas.get(e.getKey());
                int disp = r == null ? 0 : r.disponible();
                int pedida = e.getValue().cantidad();
                if (disp < pedida) {
                    faltantes.add(new ItemRestriccion(e.getValue().tipoRecurso(), pedida, disp, false, pedida - disp));
                }
            }
            if (!faltantes.isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, "Recursos insuficientes para el bloqueo", faltantes);
            }

            Compromiso c = new Compromiso();
            c.setProyectoRef(ref);
            c.setOng(ong);
            for (var e : items.entrySet()) {
                RecursoOng r = filas.get(e.getKey());
                r.setCantidadBloqueada(r.getCantidadBloqueada() + e.getValue().cantidad());
                c.agregarItem(new CompromisoItem(r.getTipoRecurso(), e.getKey(), e.getValue().cantidad()));
            }
            return new ResultadoBloqueo(aDto(compromisos.save(c)), true);
        });
    }

    /** Une items repetidos del mismo recurso sumando cantidades. */
    private static Map<String, ItemRecurso> agrupar(List<ItemRecurso> items) {
        Map<String, ItemRecurso> m = new LinkedHashMap<>();
        for (ItemRecurso i : items) {
            m.merge(Texto.clave(i.tipoRecurso()), i,
                    (a, b) -> new ItemRecurso(a.tipoRecurso(), a.cantidad() + b.cantidad()));
        }
        return m;
    }

    public static CompromisoResponse aDto(Compromiso c) {
        return new CompromisoResponse(c.getId(), c.getProyectoRef(), c.getOng().getId(), c.getOng().getNombre(),
                c.getEstado(), c.getItems().stream().map(i -> new ItemCompromiso(i.getTipoRecurso(), i.getCantidad())).toList(),
                c.getFechaCreacion(), c.getFechaLiberacion());
    }
}
