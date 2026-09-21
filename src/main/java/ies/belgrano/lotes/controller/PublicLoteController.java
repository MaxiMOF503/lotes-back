package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.request.ConsultaLoteRequest;
import ies.belgrano.lotes.dto.response.ApiErrorResponse;
import ies.belgrano.lotes.dto.response.FichaLoteResponse;
import ies.belgrano.lotes.service.LoteConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/v1/lotes")
@Tag(name = "Lotes públicos", description = "Consultas públicas de lotes demostrativos")
public class PublicLoteController {

	private final LoteConsultaService loteConsultaService;

	public PublicLoteController(LoteConsultaService loteConsultaService) {
		this.loteConsultaService = loteConsultaService;
	}

	@GetMapping("/ficha")
	@Operation(summary = "Consultar la ficha demostrativa de un lote",
			description = "Informar identificador o ambas coordenadas WGS84, nunca ambos criterios. "
					+ "La consulta por coordenadas requiere coincidencia exacta. "
					+ "Cada sección conserva su procedencia; los datos actuales son simulados y no oficiales.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lote demostrativo encontrado",
					content = @Content(schema = @Schema(implementation = FichaLoteResponse.class))),
			@ApiResponse(
					responseCode = "400",
					description = "CONSULTA_INVALIDA: parámetros inválidos o incompatibles",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(
					responseCode = "404",
					description = "LOTE_NO_ENCONTRADO: no existe un lote para el criterio indicado",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(
					responseCode = "500",
					description = "ERROR_INTERNO: falla técnica inesperada",
					content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
	})
	public ResponseEntity<FichaLoteResponse> consultarFicha(
			@Valid @ModelAttribute @ParameterObject ConsultaLoteRequest request) {
		return ResponseEntity.ok(loteConsultaService.consultar(request.toCriteria()));
	}
}
