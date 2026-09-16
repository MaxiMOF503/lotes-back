package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.entity.LoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<LoteEntity, Long> {

	Optional<LoteEntity> findByIdentificador(String identificador);

	@Query(value = """
			SELECT *
			FROM lotes
			WHERE ST_Equals(
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
