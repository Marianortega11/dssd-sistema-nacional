package com.rescuesync.nacional.service;

import com.rescuesync.nacional.domain.*;
import com.rescuesync.nacional.dto.RecursoDtos.ItemRecurso;
import com.rescuesync.nacional.dto.ValidacionDtos.*;
import com.rescuesync.nacional.repository.OngCertificacionRepository;
import com.rescuesync.nacional.repository.RecursoOngRepository;
import com.rescuesync.nacional.util.Texto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Validacion por niveles de competencia. Nunca devuelve un rechazo binario: describe que puede hacer la ONG
 * (principal, apoyo) y hasta que riesgo, para que el sistema local adapte la oferta a lotes principales o de apoyo.
 */
@Service
public class CompetenciaService {

    private final OngService ongService;
    private final OngCertificacionRepository ongCertificaciones;
    private final RecursoOngRepository recursos;

    public CompetenciaService(OngService ongService, OngCertificacionRepository ongCertificaciones,
                              RecursoOngRepository recursos) {
        this.ongService = ongService;
        this.ongCertificaciones = ongCertificaciones;
        this.recursos = recursos;
    }

    public PerfilCompetencia evaluar(EvaluacionRequest req) {
        Ong ong = ongService.resolver(req.ong());
        NivelRiesgo gravedad = NivelRiesgo.parse(req.gravedad());
        LocalDate hoy = LocalDate.now();
        List<OngCertificacion> vigentes = ongCertificaciones.findByOngId(ong.getId()).stream()
                .filter(oc -> oc.vigente(hoy)).toList();
        var inventario = recursos.findByOngIdOrderByTipoRecurso(ong.getId());

        List<CapacidadHabilitada> capacidades = new ArrayList<>();
        List<String> recomendaciones = new ArrayList<>();

        for (ItemRecurso item : req.items()) {
            String key = Texto.clave(item.tipoRecurso());
            int disponible = inventario.stream().filter(r -> r.getTipoRecursoKey().equals(key))
                    .mapToInt(RecursoOng::disponible).sum();
            boolean cubre = disponible >= item.cantidad();

            var mejor = vigentes.stream().filter(oc -> oc.getCertificacion().getTipoRecursoKey().equals(key))
                    .map(OngCertificacion::getCertificacion)
                    .max(Comparator.comparingInt(Certificacion::getNivel));

            RolPermitido rol;
            int nivel;
            NivelRiesgo riesgoMax;
            String certNombre;
            String motivo;
            if (!ong.isHabilitada()) {
                rol = RolPermitido.NO_HABILITADA;
                nivel = 0;
                riesgoMax = NivelRiesgo.BAJO;
                certNombre = null;
                motivo = "La ONG esta pendiente de habilitacion en el Sistema Nacional";
            } else if (mejor.isPresent()) {
                Certificacion c = mejor.get();
                nivel = c.getNivel();
                riesgoMax = c.getRiesgoMaximo();
                certNombre = c.getNombre();
                if (riesgoMax.ordinal() >= gravedad.ordinal()) {
                    rol = RolPermitido.PRINCIPAL;
                    motivo = "Certificacion '" + c.getNombre() + "' habilita riesgo hasta " + riesgoMax;
                } else {
                    rol = RolPermitido.APOYO;
                    motivo = "La certificacion '" + c.getNombre() + "' cubre riesgo hasta " + riesgoMax
                            + " y la emergencia es " + gravedad + ": puede participar como apoyo";
                }
            } else {
                rol = RolPermitido.APOYO;
                nivel = 0;
                riesgoMax = NivelRiesgo.BAJO;
                certNombre = null;
                motivo = "Sin certificacion vigente para este recurso: puede aportar como apoyo en riesgo BAJO";
            }
            if (rol != RolPermitido.NO_HABILITADA && !cubre) {
                recomendaciones.add("Disponibilidad insuficiente de '" + item.tipoRecurso() + "' (disponible "
                        + disponible + ", solicitado " + item.cantidad() + "): ofrecer una cantidad parcial");
            }
            if (rol == RolPermitido.APOYO) {
                recomendaciones.add("Asignar '" + item.tipoRecurso() + "' a un lote de apoyo secundario");
            }
            capacidades.add(new CapacidadHabilitada(item.tipoRecurso(), item.cantidad(), disponible, cubre, nivel,
                    Certificacion.nombreNivel(nivel), riesgoMax, rol, certNombre, motivo));
        }

        long principales = capacidades.stream().filter(c -> c.rolPermitido() == RolPermitido.PRINCIPAL).count();
        long noHab = capacidades.stream().filter(c -> c.rolPermitido() == RolPermitido.NO_HABILITADA).count();
        Resultado resultado;
        if (noHab > 0) resultado = Resultado.NO_HABILITADA;
        else if (principales == capacidades.size()) resultado = Resultado.HABILITADA_PRINCIPAL;
        else if (principales > 0) resultado = Resultado.HABILITACION_PARCIAL;
        else resultado = Resultado.HABILITADA_APOYO;

        int nivelGlobal = capacidades.stream().mapToInt(CapacidadHabilitada::nivel).max().orElse(0);
        NivelRiesgo riesgoGlobal = capacidades.stream().map(CapacidadHabilitada::riesgoMaximoPermitido)
                .max(Comparator.naturalOrder()).orElse(NivelRiesgo.BAJO);
        if (resultado == Resultado.NO_HABILITADA) {
            recomendaciones.add("Solicitar la habilitacion de la ONG antes de adjudicarle recursos");
        }
        return new PerfilCompetencia(ong.getId(), ong.getNombre(), gravedad, resultado, nivelGlobal, riesgoGlobal,
                capacidades, recomendaciones.stream().distinct().toList());
    }
}
