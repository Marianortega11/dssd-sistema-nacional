package com.rescuesync.nacional.repository;

import com.rescuesync.nacional.domain.OngCertificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OngCertificacionRepository extends JpaRepository<OngCertificacion, Long> {
    List<OngCertificacion> findByOngId(Long ongId);

    Optional<OngCertificacion> findByOngIdAndCertificacionId(Long ongId, Long certificacionId);
}
