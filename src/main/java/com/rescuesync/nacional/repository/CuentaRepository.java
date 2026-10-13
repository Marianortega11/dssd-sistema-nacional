package com.rescuesync.nacional.repository;

import com.rescuesync.nacional.domain.Cuenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    @EntityGraph(attributePaths = "ong")
    Optional<Cuenta> findByUsername(String username);

    boolean existsByUsername(String username);
}
