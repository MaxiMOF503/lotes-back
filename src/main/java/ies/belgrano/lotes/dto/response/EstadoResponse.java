package ies.belgrano.lotes.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Estado operativo del servicio. No incluye credenciales, rutas, direcciones de servidores ni datos personales:
 * solo si responde la base, qué versión del esquema tiene, si Georef contesta y qué capas territoriales hay cargadas.
 */
public record EstadoResponse(
		String estado,
		Instant fecha,
		String baseDatos,
		String esquema,
		String georef,
		List<Capa> capas,
		List<String> capasFaltantes) {

	public record Capa(String capa, boolean requerida, boolean importada, Integer registros, String importadoEn) {
	}
}
