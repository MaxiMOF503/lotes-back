package ies.belgrano.lotes.config;

import ies.belgrano.lotes.service.EstadisticaService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ConsultaEstadisticaFilterTest {
	private final EstadisticaService estadisticas = mock(EstadisticaService.class);
	private final ConsultaEstadisticaFilter filtro = new ConsultaEstadisticaFilter(estadisticas);

	private void get(String ruta, int status) throws ServletException, IOException {
		var res = new MockHttpServletResponse();
		res.setStatus(status);
		filtro.doFilter(new MockHttpServletRequest("GET", ruta), res, new MockFilterChain());
	}

	@Test void clicEnElMapaSeCuentaPorCoordenadas() throws Exception {
		get("/api/public/v1/territorio/consulta", 200);
		verify(estadisticas).registrar("CONSULTA", "COORDENADAS", "EXITO");
	}

	@Test void busquedaPorNomenclaturaSeCuentaAparte() throws Exception {
		get("/api/public/v1/territorio/parcelas", 200);
		verify(estadisticas).registrar("CONSULTA", "NOMENCLATURA", "EXITO");
	}

	@Test void direccionSeCuentaAlBuscarLasParcelasCercanas() throws Exception {
		get("/api/public/v1/territorio/parcelas/cercanas", 200);
		verify(estadisticas).registrar("CONSULTA", "DIRECCION", "EXITO");
	}

	@Test void sinResultadosYParametrosInvalidosTienenResultadoPropio() throws Exception {
		get("/api/public/v1/territorio/consulta", 404);
		get("/api/public/v1/territorio/parcelas", 400);
		verify(estadisticas).registrar("CONSULTA", "COORDENADAS", "SIN_RESULTADOS");
		verify(estadisticas).registrar("CONSULTA", "NOMENCLATURA", "INVALIDA");
	}

	@Test void unaFallaDelServidorSeCuentaComoErrorTecnicoYSePropaga() {
		var req = new MockHttpServletRequest("GET", "/api/public/v1/territorio/consulta");
		assertThatThrownBy(() -> filtro.doFilter(req, new MockHttpServletResponse(),
			(rq, rs) -> { throw new IllegalStateException("falla"); })).isInstanceOf(IllegalStateException.class);
		verify(estadisticas).registrar("CONSULTA", "COORDENADAS", "ERROR_TECNICO");
	}

	@Test void otrasRutasNoSeCuentanNiMiranParametros() throws Exception {
		get("/api/public/v1/territorio/direcciones", 200);
		get("/api/public/v1/territorio/parcelas/Parcelas_visor0.1", 200);
		get("/api/public/v1/territorio/capas/catastro", 200);
		var post = new MockHttpServletRequest("POST", "/api/public/v1/territorio/consulta");
		filtro.doFilter(post, new MockHttpServletResponse(), new MockFilterChain());
		verifyNoInteractions(estadisticas);
	}
}
