package ies.belgrano.lotes.repository;
import ies.belgrano.lotes.entity.ConsultaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface ConsultaHistorialRepository extends JpaRepository<ConsultaHistorial,Long> {
    Page<ConsultaHistorial> findByUsuarioEmail(String email,Pageable pageable);
    Optional<ConsultaHistorial> findByIdAndUsuarioEmail(Long id,String email);
}
