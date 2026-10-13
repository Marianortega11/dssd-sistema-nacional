package com.rescuesync.nacional.repository;

import com.rescuesync.nacional.domain.Compromiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {
    Optional<Compromiso> findByProyectoRefAndOngId(String proyectoRef, Long ongId);

    List<Compromiso> findByProyectoRefOrderByOngId(String proyectoRef);

    List<Compromiso> findByOngIdOrderByIdDesc(Long ongId);
}
