package com.rescuesync.nacional.repository;

import com.rescuesync.nacional.domain.RecursoOng;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecursoOngRepository extends JpaRepository<RecursoOng, Long> {
    List<RecursoOng> findByOngIdOrderByTipoRecurso(Long ongId);

    Optional<RecursoOng> findByOngIdAndTipoRecursoKey(Long ongId, String tipoRecursoKey);

    /** Filas del inventario bloqueadas (FOR UPDATE) en orden estable para evitar deadlocks. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecursoOng r where r.ong.id = :ongId and r.tipoRecursoKey in :keys order by r.id")
    List<RecursoOng> lockByOngAndKeys(@Param("ongId") Long ongId, @Param("keys") Collection<String> keys);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecursoOng r where r.ong.id = :ongId order by r.id")
    List<RecursoOng> lockAllByOng(@Param("ongId") Long ongId);

    /** [key, nombre, total, bloqueada, cantidad de ONGs] solo de ONGs habilitadas. */
    @Query("select r.tipoRecursoKey, min(r.tipoRecurso), sum(r.cantidadTotal), sum(r.cantidadBloqueada), count(r) "
            + "from RecursoOng r where r.ong.habilitada = true group by r.tipoRecursoKey order by min(r.tipoRecurso)")
    List<Object[]> disponibilidadGlobal();

    @Query("select r.tipoRecursoKey, min(r.tipoRecurso), sum(r.cantidadTotal), sum(r.cantidadBloqueada), count(r) "
            + "from RecursoOng r where r.ong.habilitada = true and r.tipoRecursoKey = :key group by r.tipoRecursoKey")
    List<Object[]> disponibilidadGlobalPorTipo(@Param("key") String key);
}
