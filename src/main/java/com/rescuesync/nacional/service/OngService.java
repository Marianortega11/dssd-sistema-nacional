package com.rescuesync.nacional.service;

import com.rescuesync.nacional.domain.*;
import com.rescuesync.nacional.dto.OngDtos.*;
import com.rescuesync.nacional.exception.ApiException;
import com.rescuesync.nacional.repository.*;
import com.rescuesync.nacional.util.Texto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OngService {

    private final OngRepository ongs;
    private final CertificacionRepository certificaciones;
    private final OngCertificacionRepository ongCertificaciones;
    private final RecursoOngRepository recursos;

    public OngService(OngRepository ongs, CertificacionRepository certificaciones,
                      OngCertificacionRepository ongCertificaciones, RecursoOngRepository recursos) {
        this.ongs = ongs;
        this.certificaciones = certificaciones;
        this.ongCertificaciones = ongCertificaciones;
        this.recursos = recursos;
    }

    /** Resuelve una ONG por alias local (username en Bonita/app local) o por CUIT. */
    public Ong resolver(String identificador) {
        String t = identificador == null ? "" : identificador.trim();
        var porAlias = ongs.findByAliasLocalIgnoreCase(t);
        if (porAlias.isPresent()) return porAlias.get();
        String digitos = Texto.soloDigitos(t);
        if (digitos.length() == 11) {
            var porCuit = ongs.findByCuit(digitos);
            if (porCuit.isPresent()) return porCuit.get();
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "No existe una ONG con alias o CUIT '" + t + "'");
    }

    public Ong obtener(Long id) {
        return ongs.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ONG inexistente: " + id));
    }

    public List<CertificacionResponse> listarCertificaciones() {
        return certificaciones.findAllByOrderByNombre().stream().map(c -> new CertificacionResponse(c.getId(),
                c.getCodigo(), c.getNombre(), c.getTipoRecurso(), c.getNivel(), Certificacion.nombreNivel(c.getNivel()),
                c.getRiesgoMaximo())).toList();
    }

    public List<OngResponse> listar(Boolean habilitada) {
        List<Ong> lista = habilitada == null ? ongs.findAllByOrderByNombre()
                : ongs.findByHabilitadaOrderByNombre(habilitada);
        return lista.stream().map(this::aDto).toList();
    }

    public OngResponse detalle(Long id) {
        return aDto(obtener(id));
    }

    @Transactional
    public OngResponse cambiarHabilitacion(Long id, boolean habilitada) {
        Ong ong = ongs.lockById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ONG inexistente: " + id));
        ong.setHabilitada(habilitada);
        return aDto(ong);
    }

    @Transactional
    public OngResponse asignarCertificacion(Long ongId, String codigo, LocalDate vigenteHasta) {
        Ong ong = obtener(ongId);
        Certificacion cert = certificaciones.findByCodigo(codigo.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Certificacion inexistente: " + codigo));
        var existente = ongCertificaciones.findByOngIdAndCertificacionId(ongId, cert.getId());
        if (existente.isPresent()) {
            existente.get().setVigenteHasta(vigenteHasta);
        } else {
            ongCertificaciones.save(new OngCertificacion(ong, cert, vigenteHasta));
        }
        return aDto(ong);
    }

    public List<RecursoOngResponse> inventario(Long ongId) {
        obtener(ongId);
        return recursos.findByOngIdOrderByTipoRecurso(ongId).stream().map(OngService::aDto).toList();
    }

    /** Reemplaza/crea las cantidades totales. No permite bajar el total por debajo de lo ya bloqueado. */
    @Transactional
    public List<RecursoOngResponse> actualizarInventario(Long ongId, List<ItemInventario> items) {
        Ong ong = ongs.lockById(ongId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ONG inexistente: " + ongId));
        Map<String, ItemInventario> porClave = new LinkedHashMap<>();
        items.forEach(i -> porClave.put(Texto.clave(i.tipoRecurso()), i));

        Map<String, RecursoOng> actuales = new LinkedHashMap<>();
        recursos.lockAllByOng(ongId).forEach(r -> actuales.put(r.getTipoRecursoKey(), r));

        for (var e : porClave.entrySet()) {
            RecursoOng r = actuales.get(e.getKey());
            int nuevoTotal = e.getValue().cantidadTotal();
            if (r == null) {
                recursos.save(new RecursoOng(ong, e.getValue().tipoRecurso(), nuevoTotal));
            } else {
                if (nuevoTotal < r.getCantidadBloqueada()) {
                    throw new ApiException(HttpStatus.CONFLICT, "No se puede fijar el total de '" + r.getTipoRecurso()
                            + "' en " + nuevoTotal + ": hay " + r.getCantidadBloqueada() + " unidades bloqueadas");
                }
                r.setCantidadTotal(nuevoTotal);
            }
        }
        return recursos.findByOngIdOrderByTipoRecurso(ongId).stream().map(OngService::aDto).toList();
    }

    private OngResponse aDto(Ong o) {
        LocalDate hoy = LocalDate.now();
        List<CertificacionOngResponse> certs = ongCertificaciones.findByOngId(o.getId()).stream()
                .map(oc -> new CertificacionOngResponse(oc.getCertificacion().getCodigo(),
                        oc.getCertificacion().getNombre(), oc.getCertificacion().getTipoRecurso(),
                        oc.getCertificacion().getNivel(), oc.getCertificacion().getRiesgoMaximo(),
                        oc.getVigenteHasta(), oc.vigente(hoy)))
                .toList();
        return new OngResponse(o.getId(), o.getNombre(), o.getCuit(), o.getEmail(), o.getAliasLocal(),
                o.isHabilitada(), certs);
    }

    private static RecursoOngResponse aDto(RecursoOng r) {
        return new RecursoOngResponse(r.getTipoRecurso(), r.getCantidadTotal(), r.getCantidadBloqueada(), r.disponible());
    }
}
