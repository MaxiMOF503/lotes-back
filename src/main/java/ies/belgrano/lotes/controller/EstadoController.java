package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.response.EstadoResponse;
import ies.belgrano.lotes.service.EstadoService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/v1/estado")
public class EstadoController {
	private final EstadoService service;

	public EstadoController(EstadoService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Estado operativo del servicio",
		description = "OK, DEGRADADO (faltan capas o Georef no responde, las consultas por mapa pueden seguir) o "
			+ "NO_DISPONIBLE (la base no responde: HTTP 503). No expone credenciales ni datos personales.")
	public ResponseEntity<EstadoResponse> estado() {
		EstadoResponse estado = service.estado();
		return ResponseEntity.status(EstadoService.NO_DISPONIBLE.equals(estado.estado()) ? 503 : 200).body(estado);
	}
}
