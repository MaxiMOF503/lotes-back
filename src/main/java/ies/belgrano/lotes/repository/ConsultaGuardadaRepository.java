package ies.belgrano.lotes.repository;
import ies.belgrano.lotes.entity.ConsultaGuardada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface ConsultaGuardadaRepository extends JpaRepository<ConsultaGuardada,Long> {
    Page<ConsultaGuardada> findByUsuarioEmail(String email,Pageable pageable);
    Optional<ConsultaGuardada> findByUsuarioEmailAndParcelaId(String email,String parcelaId);
    Optional<ConsultaGuardada> findFirstByUsuarioEmailAndParcelaIdInOrderByIdAsc(String email,java.util.Collection<String> parcelaIds);
    Optional<ConsultaGuardada> findByIdAndUsuarioEmail(Long id,String email);
}
