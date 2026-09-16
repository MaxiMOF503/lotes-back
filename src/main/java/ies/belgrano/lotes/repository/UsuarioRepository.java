package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

	Optional<UsuarioEntity> findByEmail(String email);

	boolean existsByEmail(String email);
}
