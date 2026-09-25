package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.response.SesionUsuarioResponse;
import ies.belgrano.lotes.service.UsuarioAutenticacionService;

import java.security.Principal;
import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SesionController {

	private final UsuarioAutenticacionService autenticacionService;

	public SesionController(UsuarioAutenticacionService autenticacionService) {
		this.autenticacionService = autenticacionService;
	}

	@GetMapping("/api/csrf")
	public Map<String, String> csrf(CsrfToken csrf) {
		return Map.of("headerName", csrf.getHeaderName(), "token", csrf.getToken());
	}

	@GetMapping("/api/me")
	public SesionUsuarioResponse sesion(Principal principal) {
		return autenticacionService.obtenerSesion(principal.getName());
	}

	@GetMapping("/api/admin/me")
	public SesionUsuarioResponse sesionAdmin(Principal principal) {
		return autenticacionService.obtenerSesion(principal.getName());
	}
}
