package ies.belgrano.lotes.config;

import ies.belgrano.lotes.entity.DepartamentoEntity;
import ies.belgrano.lotes.entity.LineaElectricaEntity;
import ies.belgrano.lotes.entity.LoteEntity;
import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.repository.DepartamentoRepository;
import ies.belgrano.lotes.repository.LineaElectricaRepository;
import ies.belgrano.lotes.repository.LoteRepository;
import ies.belgrano.lotes.repository.RolRepository;
import ies.belgrano.lotes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatosDemostrativosInitializerTest {

	@Mock
	private LoteRepository loteRepository;

	@Mock
	private DepartamentoRepository departamentoRepository;

	@Mock
	private RolRepository rolRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private LineaElectricaRepository lineaElectricaRepository;

	@InjectMocks
	private DatosDemostrativosInitializer initializer;

	@Test
	void creaLosCincoConjuntosDeDatosDemostrativosConGeometriasSrid4326() {
		given(rolRepository.findByNombre(any(RolEntity.NombreRol.class))).willReturn(Optional.empty());
		given(rolRepository.save(any(RolEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(usuarioRepository.existsByEmail("usuario.demo@loteseguro.invalid")).willReturn(false);
		given(departamentoRepository.findByNombre("Luján de Cuyo")).willReturn(Optional.empty());
		given(departamentoRepository.save(any(DepartamentoEntity.class)))
				.willAnswer(invocation -> invocation.getArgument(0));
		given(loteRepository.existsByIdentificador("LOT-DEMO-001")).willReturn(false);
		given(lineaElectricaRepository.existsByTipoAndFuenteDatos(
				"RED DEMOSTRATIVA", "Dataset demostrativo B-01")).willReturn(false);

		initializer.run(null);

		ArgumentCaptor<RolEntity> roles = ArgumentCaptor.forClass(RolEntity.class);
		verify(rolRepository, org.mockito.Mockito.times(2)).save(roles.capture());
		assertThat(roles.getAllValues())
				.extracting(RolEntity::getNombre)
				.containsExactly(RolEntity.NombreRol.ADMIN, RolEntity.NombreRol.USUARIO);

		ArgumentCaptor<UsuarioEntity> usuario = ArgumentCaptor.forClass(UsuarioEntity.class);
		verify(usuarioRepository).save(usuario.capture());
		assertThat(usuario.getValue().getNombre()).containsIgnoringCase("demostrativo");
		assertThat(usuario.getValue().getEmail()).endsWith(".invalid");
		assertThat(usuario.getValue().getPassword()).isEqualTo("NO-OPERATIVA-DEMO-B01-SIN-CREDENCIAL");
		assertThat(usuario.getValue().getRol().getNombre()).isEqualTo(RolEntity.NombreRol.USUARIO);

		ArgumentCaptor<LoteEntity> lote = ArgumentCaptor.forClass(LoteEntity.class);
		verify(loteRepository).save(lote.capture());
		assertThat(lote.getValue().getIdentificador()).isEqualTo("LOT-DEMO-001");
		assertThat(lote.getValue().getUbicacion().getSRID()).isEqualTo(4326);
		assertThat(lote.getValue().getUbicacion().getX()).isEqualTo(-68.8458);
		assertThat(lote.getValue().getUbicacion().getY()).isEqualTo(-32.8895);

		ArgumentCaptor<LineaElectricaEntity> linea = ArgumentCaptor.forClass(LineaElectricaEntity.class);
		verify(lineaElectricaRepository).save(linea.capture());
		assertThat(linea.getValue().getTrazado().getSRID()).isEqualTo(4326);
		assertThat(linea.getValue().getTrazado().getNumGeometries()).isEqualTo(1);
		assertThat(linea.getValue().getTrazado().getCoordinates()).hasSize(3);
		assertThat(linea.getValue().getTrazado().getCoordinates()[0].getX()).isEqualTo(-68.8480);
		assertThat(linea.getValue().getTrazado().getCoordinates()[0].getY()).isEqualTo(-32.8905);
		assertThat(linea.getValue().getFuenteDatos()).isEqualTo("Dataset demostrativo B-01");
	}

	@Test
	void noDuplicaDatosQueYaExisten() {
		RolEntity admin = new RolEntity(RolEntity.NombreRol.ADMIN);
		RolEntity usuario = new RolEntity(RolEntity.NombreRol.USUARIO);
		DepartamentoEntity departamento = new DepartamentoEntity(
				"Luján de Cuyo", null, null, null, "Dataset demostrativo B-01");

		given(rolRepository.findByNombre(RolEntity.NombreRol.ADMIN)).willReturn(Optional.of(admin));
		given(rolRepository.findByNombre(RolEntity.NombreRol.USUARIO)).willReturn(Optional.of(usuario));
		given(usuarioRepository.existsByEmail("usuario.demo@loteseguro.invalid")).willReturn(true);
		given(departamentoRepository.findByNombre("Luján de Cuyo")).willReturn(Optional.of(departamento));
		given(loteRepository.existsByIdentificador("LOT-DEMO-001")).willReturn(true);
		given(lineaElectricaRepository.existsByTipoAndFuenteDatos(
				"RED DEMOSTRATIVA", "Dataset demostrativo B-01")).willReturn(true);

		initializer.run(null);

		verify(rolRepository, never()).save(any(RolEntity.class));
		verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
		verify(departamentoRepository, never()).save(any(DepartamentoEntity.class));
		verify(loteRepository, never()).save(any(LoteEntity.class));
		verify(lineaElectricaRepository, never()).save(any(LineaElectricaEntity.class));
	}
}
