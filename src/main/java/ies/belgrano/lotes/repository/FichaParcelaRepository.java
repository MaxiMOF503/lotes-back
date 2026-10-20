package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.entity.FichaParcela;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FichaParcelaRepository extends JpaRepository<FichaParcela,String> {
    /** Direcciones relevadas por el equipo; Spring Data escapa % y _ del texto buscado. */
    List<FichaParcela> findTop5ByDireccionContainingIgnoreCaseOrderByDireccion(String texto);
}
