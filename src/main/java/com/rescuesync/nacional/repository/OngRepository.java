package com.rescuesync.nacional.repository;

import com.rescuesync.nacional.domain.Ong;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OngRepository extends JpaRepository<Ong, Long> {
    Optional<Ong> findByAliasLocalIgnoreCase(String aliasLocal);

    Optional<Ong> findByCuit(String cuit);

    boolean existsByCuit(String cuit);

    boolean existsByAliasLocalIgnoreCase(String aliasLocal);

    List<Ong> findByHabilitadaOrderByNombre(boolean habilitada);

    List<Ong> findAllByOrderByNombre();

    /** SELECT ... FOR UPDATE: punto de serializacion de todas las operaciones sobre los recursos de una ONG. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Ong o where o.id = :id")
    Optional<Ong> lockById(@Param("id") Long id);
}
