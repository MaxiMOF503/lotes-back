package ies.belgrano.lotes.dto.response;

import java.math.BigDecimal;

public record CriterioConsultaResponse(
		String tipo,
		String identificador,
		BigDecimal latitud,
		BigDecimal longitud) {
}
