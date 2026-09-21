package ies.belgrano.lotes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProcedenciaResponse(
		@Schema(example = "SIMULADO") String tipo,
		@Schema(example = "Dataset demostrativo B-01") String fuente,
		@Schema(example = "false") boolean oficial) {
}
