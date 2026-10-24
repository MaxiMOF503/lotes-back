package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.EstadoResponse;
import ies.belgrano.lotes.dto.response.EstadoResponse.Capa;
import ies.belgrano.lotes.dto.response.TerritorioResponse.Fuente;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Diagnóstico para saber rápido si falló la base, una importación o el servicio de direcciones. */
@Service
public class EstadoService {
	public static final String OK = "OK";
	public static final String DEGRADADO = "DEGRADADO";
	public static final String NO_DISPONIBLE = "NO_DISPONIBLE";
	/** Capas sin las cuales las consultas del mapa no funcionan. */
	static final List<String> CAPAS_REQUERIDAS = List.of("catastro", "aysam", "electricidad");
	private static final Duration VIGENCIA_GEOREF = Duration.ofSeconds(60);

	private final JdbcTemplate jdbc;
	private final TerritorioService territorio;
	private final RestClient georef;
	private final Clock reloj;
	private Instant georefVerificadoEn = Instant.MIN;
	private boolean georefDisponible;

	public EstadoService(JdbcTemplate jdbc, TerritorioService territorio, RestClient georef, Clock reloj) {
		this.jdbc = jdbc;
		this.territorio = territorio;
		this.georef = georef;
		this.reloj = reloj;
	}

	public EstadoResponse estado() {
		Instant ahora = reloj.instant();
		if (!baseResponde()) {
			return new EstadoResponse(NO_DISPONIBLE, ahora, NO_DISPONIBLE, null, "NO_VERIFICADO", List.of(), CAPAS_REQUERIDAS);
		}
		List<Capa> capas = capas();
		List<String> faltantes = capas.stream().filter(c -> c.requerida() && !c.importada()).map(Capa::capa).toList();
		boolean georefOk = georefDisponible();
		String estado = faltantes.isEmpty() && georefOk ? OK : DEGRADADO;
		return new EstadoResponse(estado, ahora, OK, versionDelEsquema(), georefOk ? "DISPONIBLE" : NO_DISPONIBLE, capas, faltantes);
	}

	private boolean baseResponde() {
		try {
			jdbc.queryForObject("SELECT 1", Integer.class);
			return true;
		} catch (DataAccessException | org.springframework.transaction.TransactionException e) {
			return false;
		}
	}

	private String versionDelEsquema() {
		try {
			return jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success=1 AND version IS NOT NULL "
				+ "ORDER BY installed_rank DESC LIMIT 1", String.class);
		} catch (DataAccessException e) {
			return null;
		}
	}

	private List<Capa> capas() {
		List<Fuente> fuentes = fuentesImportadas();
		List<Capa> resultado = new ArrayList<>();
		for (String requerida : CAPAS_REQUERIDAS) {
			resultado.add(capa(requerida, true, fuentes));
		}
		fuentes.stream().map(Fuente::capa).filter(c -> !CAPAS_REQUERIDAS.contains(c))
			.forEach(c -> resultado.add(capa(c, false, fuentes)));
		return resultado;
	}

	private List<Fuente> fuentesImportadas() {
		try {
			return territorio.fuentes();
		} catch (OperacionInvalidaException e) {
			return List.of();  // la tabla de capas todavía no existe: nada fue importado
		}
	}

	private static Capa capa(String nombre, boolean requerida, List<Fuente> fuentes) {
		return fuentes.stream().filter(f -> f.capa().equals(nombre)).findFirst()
			.map(f -> new Capa(nombre, requerida, true, f.registros(), f.importadoEn()))
			.orElseGet(() -> new Capa(nombre, requerida, false, null, null));
	}

	/** Georef es externo: se consulta como mucho una vez por minuto para no sumar carga ni demoras. */
	private synchronized boolean georefDisponible() {
		Instant ahora = reloj.instant();
		if (Duration.between(georefVerificadoEn, ahora).compareTo(VIGENCIA_GEOREF) < 0) return georefDisponible;
		try {
			georef.get().uri("/provincias?max=1&campos=id").retrieve().toBodilessEntity();
			georefDisponible = true;
		} catch (RestClientException e) {
			georefDisponible = false;
		}
		georefVerificadoEn = ahora;
		return georefDisponible;
	}
}
