package ies.belgrano.lotes.dto.response;

public record ZonificacionResponse(
		String descripcion,
		boolean verificada,
		ProcedenciaResponse procedencia) {
}
