package ies.belgrano.lotes.dto.response;

import java.math.BigDecimal;

public record CriterioConsultaResponse(
		String tipo,
		String identificador,
		BigDecimal latitud,
		BigDecimal longitud,
		String direccion) {

	public CriterioConsultaResponse(String tipo, String identificador, BigDecimal latitud, BigDecimal longitud) {
		this(tipo, identificador, latitud, longitud, null);
	}
}
