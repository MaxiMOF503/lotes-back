package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<RolEntity, Long> {

	Optional<RolEntity> findByNombre(RolEntity.NombreRol nombre);
}
