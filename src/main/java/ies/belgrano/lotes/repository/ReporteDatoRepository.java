package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.entity.ReporteDato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteDatoRepository extends JpaRepository<ReporteDato, Long> {
    Page<ReporteDato> findByCreadoPorEmail(String email, Pageable pageable);
    Page<ReporteDato> findByEstado(ReporteDato.Estado estado, Pageable pageable);
}
