package ies.belgrano.lotes.model;

import java.math.BigDecimal;

public record ConsultaLoteCriteria(
		TipoCriterio tipo,
		String identificador,
		BigDecimal latitud,
		BigDecimal longitud,
		String direccion) {

	public static ConsultaLoteCriteria porIdentificador(String identificador) {
		return new ConsultaLoteCriteria(TipoCriterio.IDENTIFICADOR, identificador, null, null, null);
	}

	public static ConsultaLoteCriteria porDireccion(String direccion) {
		return new ConsultaLoteCriteria(TipoCriterio.DIRECCION, null, null, null, direccion);
	}

	public static ConsultaLoteCriteria porCoordenadas(BigDecimal latitud, BigDecimal longitud) {
		return new ConsultaLoteCriteria(TipoCriterio.COORDENADAS, null, latitud, longitud, null);
	}

	public enum TipoCriterio {
		IDENTIFICADOR,
		DIRECCION,
		COORDENADAS
	}
}
