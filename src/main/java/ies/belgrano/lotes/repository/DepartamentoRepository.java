package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.entity.DepartamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartamentoRepository extends JpaRepository<DepartamentoEntity, Long> {

	Optional<DepartamentoEntity> findByNombre(String nombre);
}
