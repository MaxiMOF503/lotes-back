package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.response.AguaResponse;
import ies.belgrano.lotes.dto.response.CoordenadasResponse;
import ies.belgrano.lotes.dto.response.CriterioConsultaResponse;
import ies.belgrano.lotes.dto.response.DatosLoteResponse;
import ies.belgrano.lotes.dto.response.ElectricidadResponse;
import ies.belgrano.lotes.dto.response.FichaLoteResponse;
import ies.belgrano.lotes.dto.response.ProcedenciaResponse;
import ies.belgrano.lotes.dto.response.ZonificacionResponse;
import ies.belgrano.lotes.exception.LoteNoEncontradoException;
import ies.belgrano.lotes.model.ConsultaLoteCriteria;
import ies.belgrano.lotes.service.LoteConsultaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicLoteController.class)
class PublicLoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LoteConsultaService loteConsultaService;

	@BeforeEach
	void setUp() {
		given(loteConsultaService.consultar(any(ConsultaLoteCriteria.class)))
				.willReturn(respuestaDemostrativa());
	}

	@Test
	void consultaValidaPorIdentificadorDevuelveFichaCompletaYSimulada() throws Exception {
		mockMvc.perform(get("/api/public/v1/lotes/ficha")
						.param("identificador", "LOT-DEMO-001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lote.identificador").value("LOT-DEMO-001"))
				.andExpect(jsonPath("$.zonificacion.descripcion").value("Residencial R2 (dato simulado)"))
				.andExpect(jsonPath("$.zonificacion.verificada").value(false))
				.andExpect(jsonPath("$.infraestructuraElectrica.distanciaRedElectricaMts").value(320.0))
				.andExpect(jsonPath("$.infraestructuraElectrica.tieneAcceso").value(true))
				.andExpect(jsonPath("$.coberturaAgua.tieneCobertura").value(true))
				.andExpect(jsonPath("$.lote.procedencia.tipo").value("SIMULADO"))
				.andExpect(jsonPath("$.lote.procedencia.fuente").value("Dataset demostrativo B-01"))
				.andExpect(jsonPath("$.lote.procedencia.oficial").value(false))
				.andExpect(jsonPath("$.zonificacion.procedencia.tipo").value("SIMULADO"))
				.andExpect(jsonPath("$.infraestructuraElectrica.procedencia.tipo").value("SIMULADO"))
				.andExpect(jsonPath("$.coberturaAgua.procedencia.tipo").value("SIMULADO"))
				.andExpect(jsonPath("$.advertencia").isNotEmpty());
	}

	@Test
	void consultaValidaPorCoordenadasDevuelveFicha() throws Exception {
		mockMvc.perform(get("/api/public/v1/lotes/ficha")
						.param("latitud", "-32.8895")
						.param("longitud", "-68.8458"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lote.identificador").value("LOT-DEMO-001"));
	}

	@Test
	void consultaSinCriterioDevuelveBadRequest() throws Exception {
		mockMvc.perform(get("/api/public/v1/lotes/ficha"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("CONSULTA_INVALIDA"));

		verifyNoInteractions(loteConsultaService);
	}

	@Test
	void identificadorYCoordenadasDevuelveBadRequest() throws Exception {
		mockMvc.perform(get("/api/public/v1/lotes/ficha")
						.param("identificador", "LOT-DEMO-001")
						.param("latitud", "-32.8895")
						.param("longitud", "-68.8458"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("CONSULTA_INVALIDA"));
	}

	@Test
	void coordenadasIncompletasDevuelveBadRequest() throws Exception {
		mockMvc.perform(get("/api/public/v1/lotes/ficha")
						.param("latitud", "-32.8895"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("CONSULTA_INVALIDA"));
	}

	@Test
	void coordenadasFueraDeRangoDevuelveBadRequest() throws Exception {
		mockMvc.perform(get("/api/public/v1/lotes/ficha")
						.param("latitud", "91")
						.param("longitud", "-68.8458"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("CONSULTA_INVALIDA"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'latitud')]").exists());
	}

	@Test
	void loteInexistenteDevuelveNotFound() throws Exception {
		given(loteConsultaService.consultar(any(ConsultaLoteCriteria.class)))
				.willThrow(new LoteNoEncontradoException());

		mockMvc.perform(get("/api/public/v1/lotes/ficha")
						.param("identificador", "NO-EXISTE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("LOTE_NO_ENCONTRADO"));
	}

	private FichaLoteResponse respuestaDemostrativa() {
		ProcedenciaResponse procedencia =
				new ProcedenciaResponse("SIMULADO", "Dataset demostrativo B-01", false);
		return new FichaLoteResponse(
				new CriterioConsultaResponse("IDENTIFICADOR", "LOT-DEMO-001", null, null),
				new DatosLoteResponse(
						"LOT-DEMO-001",
						"Calle Demostración 123, Mendoza",
						"Luján de Cuyo",
						new CoordenadasResponse(
								new BigDecimal("-32.889500"), new BigDecimal("-68.845800")),
						procedencia),
				new ZonificacionResponse(
						"Residencial R2 (dato simulado)", false, procedencia),
				new ElectricidadResponse(320.0, true, procedencia),
				new AguaResponse(true, procedencia),
				LoteConsultaService.ADVERTENCIA,
				null);
	}
}
