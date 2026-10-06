package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.request.RegistroUsuarioRequest;
import ies.belgrano.lotes.service.RegistroUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/v1/usuarios")
public class RegistroUsuarioController {
	private final RegistroUsuarioService registro;

	public RegistroUsuarioController(RegistroUsuarioService registro) {
		this.registro = registro;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void registrar(@Valid @RequestBody RegistroUsuarioRequest solicitud) {
		registro.registrar(solicitud);
	}
}
