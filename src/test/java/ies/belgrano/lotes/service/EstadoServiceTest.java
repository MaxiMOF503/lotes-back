package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.TerritorioResponse.Fuente;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EstadoServiceTest {
	private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
	private final TerritorioService territorio = mock(TerritorioService.class);
	private MockRestServiceServer georef;
	private RelojPrueba reloj;
	private EstadoService service;

	static class RelojPrueba extends Clock {
		Instant ahora = Instant.parse("2026-10-24T12:00:00Z");
		public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
		public Clock withZone(java.time.ZoneId z) { return this; }
		public Instant instant() { return ahora; }
	}

	private static Fuente fuente(String capa) { return new Fuente(capa, "Fuente", "https://x.invalid", 10, 0, 0, "2026-10-01"); }

	@BeforeEach void preparar() {
		var builder = RestClient.builder().baseUrl("https://georef.test/api");
		georef = MockRestServiceServer.bindTo(builder).build();
		reloj = new RelojPrueba();
		service = new EstadoService(jdbc, territorio, builder.build(), reloj);
		when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
		when(jdbc.queryForObject(contains("flyway_schema_history"), eq(String.class))).thenReturn("3");
	}

	@Test void todoDisponibleEsOkYMuestraLaVersionDelEsquema() {
		when(territorio.fuentes()).thenReturn(List.of(fuente("catastro"), fuente("aysam"), fuente("electricidad")));
		georef.expect(requestTo("https://georef.test/api/provincias?max=1&campos=id")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		var estado = service.estado();
		assertThat(estado.estado()).isEqualTo("OK");
		assertThat(estado.baseDatos()).isEqualTo("OK");
		assertThat(estado.esquema()).isEqualTo("3");
		assertThat(estado.georef()).isEqualTo("DISPONIBLE");
		assertThat(estado.capasFaltantes()).isEmpty();
		assertThat(estado.capas()).extracting(c -> c.capa() + ":" + c.importada()).containsExactly("catastro:true", "aysam:true", "electricidad:true");
	}

	@Test void unaCapaSinImportarDejaElServicioDegradadoYLaNombra() {
		when(territorio.fuentes()).thenReturn(List.of(fuente("catastro"), fuente("aysam")));
		georef.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/provincias"))).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		var estado = service.estado();
		assertThat(estado.estado()).isEqualTo("DEGRADADO");
		assertThat(estado.capasFaltantes()).containsExactly("electricidad");
		assertThat(estado.capas()).filteredOn(c -> c.capa().equals("electricidad")).singleElement().satisfies(c -> {
			assertThat(c.importada()).isFalse();
			assertThat(c.registros()).isNull();
		});
	}

	@Test void sinTablaDeCapasTodasFaltan() {
		when(territorio.fuentes()).thenThrow(new OperacionInvalidaException(503, "TERRITORIO_NO_IMPORTADO", "No importado"));
		georef.expect(requestTo(org.hamcrest.Matchers.containsString("/provincias"))).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		var estado = service.estado();
		assertThat(estado.estado()).isEqualTo("DEGRADADO");
		assertThat(estado.capasFaltantes()).containsExactly("catastro", "aysam", "electricidad");
	}

	@Test void georefCaidoDegradaPeroNoApagaElServicioYSeConsultaUnaVezPorMinuto() {
		when(territorio.fuentes()).thenReturn(List.of(fuente("catastro"), fuente("aysam"), fuente("electricidad")));
		georef.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/provincias"))).andRespond(withServerError());
		assertThat(service.estado().georef()).isEqualTo("NO_DISPONIBLE");
		assertThat(service.estado().estado()).isEqualTo("DEGRADADO");   // dentro del minuto: no vuelve a llamar
		georef.verify();
		reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(61));
		georef.reset();
		georef.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/provincias"))).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		assertThat(service.estado().georef()).isEqualTo("DISPONIBLE");
		georef.verify();
	}

	@Test void conLaBaseCaidaNoConsultaNadaMasYInformaNoDisponibleSinExponerElError() {
		when(jdbc.queryForObject("SELECT 1", Integer.class)).thenThrow(new DataAccessResourceFailureException("jdbc:mysql://servidor-secreto usuario=root"));
		var estado = service.estado();
		assertThat(estado.estado()).isEqualTo("NO_DISPONIBLE");
		assertThat(estado.baseDatos()).isEqualTo("NO_DISPONIBLE");
		assertThat(estado.toString()).doesNotContain("secreto");
		verifyNoInteractions(territorio);
		georef.verify();
	}
}
