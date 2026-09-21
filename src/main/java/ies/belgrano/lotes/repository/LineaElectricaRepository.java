package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.entity.LineaElectricaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineaElectricaRepository extends JpaRepository<LineaElectricaEntity, Long> {

	boolean existsByTipoAndFuenteDatos(String tipo, String fuenteDatos);
}
