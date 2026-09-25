package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.SesionUsuarioResponse;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAutenticacionService implements UserDetailsService {

	private final UsuarioRepository usuarios;

	public UsuarioAutenticacionService(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		var usuario = usuarios.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
		return User.withUsername(usuario.getEmail())
				.password(usuario.getPassword())
				.roles(usuario.getRol().getNombre().name())
				.build();
	}

	@Transactional(readOnly = true)
	public SesionUsuarioResponse obtenerSesion(String email) {
		var usuario = usuarios.findByEmail(email)
				.orElseThrow(() -> new OperacionInvalidaException(
						401, "AUTENTICACION_REQUERIDA", "La sesión ya no corresponde a un usuario válido"));
		return new SesionUsuarioResponse(
				usuario.getNombre(),
				usuario.getEmail(),
				usuario.getRol().getNombre().name());
	}
}
