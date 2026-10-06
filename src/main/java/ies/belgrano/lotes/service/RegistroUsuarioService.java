package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.request.RegistroUsuarioRequest;
import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.RolRepository;
import ies.belgrano.lotes.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroUsuarioService {
	private final UsuarioRepository usuarios;
	private final RolRepository roles;
	private final PasswordEncoder encoder;

	public RegistroUsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder) {
		this.usuarios = usuarios;
		this.roles = roles;
		this.encoder = encoder;
	}

	@Transactional
	public void registrar(RegistroUsuarioRequest solicitud) {
		String email = solicitud.email().trim().toLowerCase(Locale.ROOT);
		if (solicitud.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new OperacionInvalidaException(400, "PASSWORD_INVALIDA",
					"La contraseña no puede superar 72 bytes en UTF-8");
		}
		if (usuarios.existsByEmailIgnoreCase(email)) {
			throw emailEnUso();
		}
		RolEntity rol = roles.findByNombre(RolEntity.NombreRol.USUARIO)
				.orElseThrow(() -> new IllegalStateException("Falta el rol USUARIO"));
		try {
			usuarios.saveAndFlush(new UsuarioEntity(
					solicitud.nombre().trim(), email, encoder.encode(solicitud.password()), rol));
		} catch (DataIntegrityViolationException exception) {
			throw emailEnUso();
		}
	}

	private OperacionInvalidaException emailEnUso() {
		return new OperacionInvalidaException(409, "EMAIL_EN_USO", "Ya existe una cuenta con ese correo");
	}
}
