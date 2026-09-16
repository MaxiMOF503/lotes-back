package ies.belgrano.lotes.dto.response;

public record DatosLoteResponse(
		String identificador,
		String direccionAproximada,
		String departamento,
		CoordenadasResponse coordenadas,
		ProcedenciaResponse procedencia) {
}
