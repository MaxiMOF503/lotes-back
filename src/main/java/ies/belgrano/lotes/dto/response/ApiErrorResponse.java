package ies.belgrano.lotes.dto.response;

import org.slf4j.MDC;
import java.time.Instant;
import java.util.List;

/**
 * Formato único de error de la API. {@code codigo} es estable (ver docs/errores-api.md);
 * {@code mensaje} está pensado para mostrarse a la persona; {@code correlacionId} identifica la
 * solicitud en el log del servidor y viaja también en el encabezado X-Correlation-Id.
 */
public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String codigo,
		String mensaje,
		String path,
		List<DetalleError> errores,
		String correlacionId) {

	/** Toma el identificador de correlación de la solicitud en curso. */
	public ApiErrorResponse(Instant timestamp, int status, String codigo, String mensaje, String path,
			List<DetalleError> errores) {
		this(timestamp, status, codigo, mensaje, path, errores, MDC.get("correlacionId"));
	}

	public record DetalleError(String campo, String mensaje) {
	}
}
