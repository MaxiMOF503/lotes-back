package ies.belgrano.lotes.model;

import java.math.BigDecimal;

public record ConsultaLoteCriteria(
		TipoCriterio tipo,
		String identificador,
		BigDecimal latitud,
		BigDecimal longitud) {

	public static ConsultaLoteCriteria porIdentificador(String identificador) {
		return new ConsultaLoteCriteria(TipoCriterio.IDENTIFICADOR, identificador, null, null);
	}

	public static ConsultaLoteCriteria porCoordenadas(BigDecimal latitud, BigDecimal longitud) {
		return new ConsultaLoteCriteria(TipoCriterio.COORDENADAS, null, latitud, longitud);
	}

	public enum TipoCriterio {
		IDENTIFICADOR,
		COORDENADAS
	}
}
