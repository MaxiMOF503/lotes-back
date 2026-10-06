package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.request.RegistroUsuarioRequest;
import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.RolRepository;
import ies.belgrano.lotes.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroUsuarioServiceTest {
	@Mock UsuarioRepository usuarios;
	@Mock RolRepository roles;
	@Mock PasswordEncoder encoder;
	@InjectMocks RegistroUsuarioService registro;

	@Test
	void guardaUnUsuarioComunConEmailNormalizadoYPasswordCifrada() {
		when(roles.findByNombre(RolEntity.NombreRol.USUARIO))
				.thenReturn(Optional.of(new RolEntity(RolEntity.NombreRol.USUARIO)));
		when(encoder.encode("ClaveDePrueba123")).thenReturn("hash-bcrypt-de-prueba");

		registro.registrar(new RegistroUsuarioRequest("  Ana Pérez  ", "  ANA@EJEMPLO.COM  ", "ClaveDePrueba123"));

		ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
		verify(usuarios).saveAndFlush(captor.capture());
		UsuarioEntity usuario = captor.getValue();
		assertThat(usuario.getNombre()).isEqualTo("Ana Pérez");
		assertThat(usuario.getEmail()).isEqualTo("ana@ejemplo.com");
		assertThat(usuario.getPassword()).isEqualTo("hash-bcrypt-de-prueba");
		assertThat(usuario.getRol().getNombre()).isEqualTo(RolEntity.NombreRol.USUARIO);
	}

	@Test
	void rechazaEmailExistenteSinGuardarOtroUsuario() {
		when(usuarios.existsByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(true);
		assertThatThrownBy(() -> registro.registrar(new RegistroUsuarioRequest(
				"Ana", "ANA@EJEMPLO.COM", "ClaveDePrueba123")))
				.isInstanceOfSatisfying(OperacionInvalidaException.class,
						exception -> assertThat(exception.status).isEqualTo(409));
		verify(usuarios, never()).saveAndFlush(any());
	}
}
