package ies.belgrano.lotes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Orientación para consultar al departamento. Los contactos actuales son demostrativos, no oficiales.")
public record DondeConsultarResponse(
		String departamento,
		String contacto,
		String direccionOficina,
		String comoConsultar,
		ProcedenciaResponse procedencia) {
}
