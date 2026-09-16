package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.FichaLoteResponse;
import ies.belgrano.lotes.entity.DepartamentoEntity;
import ies.belgrano.lotes.entity.LoteEntity;
import ies.belgrano.lotes.exception.LoteNoEncontradoException;
import ies.belgrano.lotes.model.ConsultaLoteCriteria;
import ies.belgrano.lotes.repository.LoteRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoteConsultaServiceTest {
	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Mock
	private LoteRepository loteRepository;

	@InjectMocks
	private LoteConsultaService service;

	@Test
	void consultaPorIdentificadorComponeLaFichaFueraDelController() {
		given(loteRepository.findByIdentificador("LOT-DEMO-001"))
				.willReturn(Optional.of(loteDemostrativo()));

		FichaLoteResponse response = service.consultar(
				ConsultaLoteCriteria.porIdentificador("LOT-DEMO-001"));

		assertThat(response.lote().identificador()).isEqualTo("LOT-DEMO-001");
		assertThat(response.zonificacion().descripcion()).isEqualTo("Residencial R2 (dato simulado)");
		assertThat(response.zonificacion().verificada()).isFalse();
		assertThat(response.infraestructuraElectrica().distanciaRedElectricaMts()).isEqualTo(320.0);
		assertThat(response.infraestructuraElectrica().tieneAcceso()).isTrue();
		assertThat(response.coberturaAgua().tieneCobertura()).isTrue();
		assertThat(response.lote().procedencia().tipo()).isEqualTo("SIMULADO");
		assertThat(response.lote().procedencia().oficial()).isFalse();
	}

	@Test
	void consultaPorCoordenadasUsaCoincidenciaExacta() {
		BigDecimal latitud = new BigDecimal("-32.8895");
		BigDecimal longitud = new BigDecimal("-68.8458");
		given(loteRepository.findByCoordenadasExactas(latitud, longitud))
				.willReturn(Optional.of(loteDemostrativo()));

		FichaLoteResponse response = service.consultar(
				ConsultaLoteCriteria.porCoordenadas(latitud, longitud));

		assertThat(response.criterioConsulta().tipo()).isEqualTo("COORDENADAS");
		assertThat(response.lote().identificador()).isEqualTo("LOT-DEMO-001");
	}

	@Test
	void criterioValidoSinCoincidenciaLanzaLoteNoEncontrado() {
		given(loteRepository.findByIdentificador("NO-EXISTE")).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.consultar(
				ConsultaLoteCriteria.porIdentificador("NO-EXISTE")))
				.isInstanceOf(LoteNoEncontradoException.class);
	}

	private LoteEntity loteDemostrativo() {
		Point ubicacion = GEOMETRY_FACTORY.createPoint(new Coordinate(-68.8458, -32.8895));
		DepartamentoEntity departamento = new DepartamentoEntity(
				"Luján de Cuyo",
				"Contacto demostrativo no oficial",
				"Oficina demostrativa",
				"Consultar ante el organismo municipal competente",
				"Dataset demostrativo B-01");

		return new LoteEntity(
				"LOT-DEMO-001",
				ubicacion,
				departamento,
				"Calle Demostración 123, Mendoza",
				"Residencial R2 (dato simulado)",
				false,
				320.0,
				true,
				true,
				"Titular demostrativo no oficial",
				"Situación dominial simulada sin valor legal",
				true);
	}
}
