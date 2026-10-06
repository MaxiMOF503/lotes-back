package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.entity.LoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoteRepository extends JpaRepository<LoteEntity, Long> {

	Optional<LoteEntity> findByIdentificador(String identificador);
	Optional<LoteEntity> findByIdentificadorAndEliminadoFalse(String identificador);
	Optional<LoteEntity> findByIdAndEliminadoFalse(Long id);
	Page<LoteEntity> findByEliminadoFalse(Pageable pageable);

	@Query(value = """
			SELECT * FROM lotes
			WHERE direccion_aproximada IS NOT NULL
			  AND eliminado = 0
			  AND TRIM(REGEXP_REPLACE(CONVERT(direccion_aproximada USING utf8mb4), '[[:space:]]+', ' ')) COLLATE utf8mb4_0900_ai_ci
			      LIKE CONCAT('%', CONVERT(:direccion USING utf8mb4) COLLATE utf8mb4_0900_ai_ci, '%')
			ORDER BY direccion_aproximada, identificador
			LIMIT :limite
			""", nativeQuery = true)
	List<LoteEntity> buscarPorDireccion(@Param("direccion") String direccion, @Param("limite") int limite);

	@Query(value = """
			SELECT *
			FROM lotes
			WHERE eliminado = 0 AND ST_Equals(
				ubicacion,
				ST_SRID(POINT(:longitud, :latitud), 4326)
			) = 1
			LIMIT 1
			""", nativeQuery = true)
	Optional<LoteEntity> findByCoordenadasExactas(
			@Param("latitud") BigDecimal latitud,
			@Param("longitud") BigDecimal longitud);

	boolean existsByIdentificador(String identificador);
}
