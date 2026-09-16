package ies.belgrano.lotes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ficha pública demostrativa de un lote")
public record FichaLoteResponse(
		CriterioConsultaResponse criterioConsulta,
		DatosLoteResponse lote,
		ZonificacionResponse zonificacion,
		ElectricidadResponse infraestructuraElectrica,
		AguaResponse coberturaAgua,
		String advertencia) {
}
