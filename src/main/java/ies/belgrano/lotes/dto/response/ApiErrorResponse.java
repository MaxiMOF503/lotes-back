package ies.belgrano.lotes.dto.response;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String codigo,
		String mensaje,
		String path,
		List<DetalleError> errores) {

	public record DetalleError(String campo, String mensaje) {
	}
}
