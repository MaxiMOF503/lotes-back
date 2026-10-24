package ies.belgrano.lotes.config;

import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RolesInitializerTest {
	private final RolRepository roles = mock(RolRepository.class);

	@Test void creaLosDosRolesQueFaltan() {
		when(roles.findByNombre(any())).thenReturn(Optional.empty());
		new RolesInitializer(roles).run(new DefaultApplicationArguments());
		verify(roles, times(2)).save(any(RolEntity.class));
	}

	@Test void noDuplicaRolesExistentes() {
		when(roles.findByNombre(any())).thenReturn(Optional.of(new RolEntity(RolEntity.NombreRol.ADMIN)));
		new RolesInitializer(roles).run(new DefaultApplicationArguments());
		verify(roles, never()).save(any());
	}
}
