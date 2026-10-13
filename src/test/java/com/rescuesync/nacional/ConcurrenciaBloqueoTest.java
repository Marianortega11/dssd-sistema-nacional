package com.rescuesync.nacional;

import com.rescuesync.nacional.dto.RecursoDtos.ItemRecurso;
import com.rescuesync.nacional.dto.RecursoDtos.SolicitudRecursos;
import com.rescuesync.nacional.exception.ApiException;
import com.rescuesync.nacional.repository.RecursoOngRepository;
import com.rescuesync.nacional.service.ProyectoService;
import com.rescuesync.nacional.service.RecursoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcurrenciaBloqueoTest {

    @Autowired RecursoService recursos;
    @Autowired ProyectoService proyectos;
    @Autowired RecursoOngRepository recursoRepo;
    @Autowired com.rescuesync.nacional.repository.OngRepository ongRepo;

    @Test
    void bloqueosSimultaneosNoSobreAsignanYElCierreLibera() throws Exception {
        // "helen.kelly" (Bomberos) tiene 40 Brigadistas; 10 proyectos piden 5 cada uno => solo 8 caben.
        int hilos = 10;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();

        for (int i = 0; i < hilos; i++) {
            final String ref = "CONC-" + i;
            futuros.add(pool.submit(() -> {
                largada.await();
                try {
                    recursos.bloquear(new SolicitudRecursos(ref, "helen.kelly",
                            List.of(new ItemRecurso("Brigadistas", 5))));
                    ok.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getStatus().value() == 409) conflictos.incrementAndGet();
                    else throw e;
                }
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> f : futuros) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(8, ok.get());
        assertEquals(2, conflictos.get());
        assertEquals(40, bloqueadoBrigadistas());

        // Idempotencia: repetir un bloqueo ya existente no vuelve a descontar.
        for (int i = 0; i < hilos; i++) {
            try {
                recursos.bloquear(new SolicitudRecursos("CONC-" + i, "helen.kelly",
                        List.of(new ItemRecurso("Brigadistas", 5))));
            } catch (ApiException ignored) {
                // los 2 que no cupieron siguen sin caber
            }
        }
        assertEquals(40, bloqueadoBrigadistas());

        // Cierre de todos los proyectos libera todo.
        for (int i = 0; i < hilos; i++) {
            try {
                proyectos.cerrar("CONC-" + i);
            } catch (ApiException ignored) {
                // proyectos sin compromiso (los 2 rechazados)
            }
        }
        assertEquals(0, bloqueadoBrigadistas());
    }

    private int bloqueadoBrigadistas() {
        Long ongId = ongRepo.findByAliasLocalIgnoreCase("helen.kelly").orElseThrow().getId();
        return recursoRepo.findByOngIdAndTipoRecursoKey(ongId, "brigadistas").orElseThrow().getCantidadBloqueada();
    }
}
