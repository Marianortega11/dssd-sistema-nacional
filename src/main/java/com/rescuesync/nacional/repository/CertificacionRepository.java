package com.rescuesync.nacional.repository;

import com.rescuesync.nacional.domain.Certificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificacionRepository extends JpaRepository<Certificacion, Long> {
    Optional<Certificacion> findByCodigo(String codigo);

    List<Certificacion> findAllByOrderByNombre();
}
