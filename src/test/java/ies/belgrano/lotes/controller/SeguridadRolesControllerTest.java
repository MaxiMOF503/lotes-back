package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.config.SecurityConfig;
import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.repository.UsuarioRepository;
import ies.belgrano.lotes.service.EstadisticaService;
import ies.belgrano.lotes.service.RegistroUsuarioService;
import ies.belgrano.lotes.service.UsuarioAutenticacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SesionController.class, EstadisticaController.class, RegistroUsuarioController.class})
@Import({SecurityConfig.class, UsuarioAutenticacionService.class})
class SeguridadRolesControllerTest {

	private static final String PASSWORD = "PasswordSoloPruebas123";
	private static final String ADMIN_EMAIL = "admin.test@lotes.invalid";
	private static final String USUARIO_EMAIL = "usuario.test@lotes.invalid";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private UsuarioRepository usuarios;

	@MockitoBean
	private EstadisticaService estadisticaService;

	@MockitoBean
	private RegistroUsuarioService registroUsuarioService;

	@Test
	void registroPublicoRequiereCsrfYNoAsignaRolDesdeLaSolicitud() throws Exception {
		String solicitud = """
				{"nombre":"Ana","email":"ana@ejemplo.com","password":"ClaveDePrueba123","rol":"ADMIN"}
				""";
		mvc.perform(post("/api/public/v1/usuarios")
					.contentType(MediaType.APPLICATION_JSON).content(solicitud))
				.andExpect(status().isForbidden());
		mvc.perform(post("/api/public/v1/usuarios").with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(solicitud))
				.andExpect(status().isCreated());
	}

	@BeforeEach
	void configurarUsuariosYRespuestas() {
		var admin = new UsuarioEntity(
				"Administrador de prueba",
				ADMIN_EMAIL,
				passwordEncoder.encode(PASSWORD),
				new RolEntity(RolEntity.NombreRol.ADMIN));
		var usuario = new UsuarioEntity(
				"Usuario de prueba",
				USUARIO_EMAIL,
				passwordEncoder.encode(PASSWORD),
				new RolEntity(RolEntity.NombreRol.USUARIO));

		given(usuarios.findByEmail(ADMIN_EMAIL)).willReturn(Optional.of(admin));
		given(usuarios.findByEmail(USUARIO_EMAIL)).willReturn(Optional.of(usuario));

		given(estadisticaService.consultar(any(LocalDate.class), any(LocalDate.class)))
				.willReturn(new EstadisticaService.Resumen(
						LocalDate.of(2026, 9, 1),
						LocalDate.of(2026, 9, 2),
						"America/Argentina/Buenos_Aires",
						0,
						0,
						List.of()));
	}

	@Test
	void recursosInternosSinSesionDevuelven401ConFormatoApiError() throws Exception {
		mvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("AUTENTICACION_REQUERIDA"))
				.andExpect(jsonPath("$.path").value("/api/me"));

		mvc.perform(get("/api/admin/estadisticas").param("desde", "2026-09-01").param("hasta", "2026-09-02"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("AUTENTICACION_REQUERIDA"))
				.andExpect(jsonPath("$.path").value("/api/admin/estadisticas"));
	}

	@Test
	void usuarioIniciaSesionConsultaSuPerfilYCierraSesion() throws Exception {
		MockHttpSession session = iniciarSesion(USUARIO_EMAIL);

		mvc.perform(get("/api/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Usuario de prueba"))
				.andExpect(jsonPath("$.email").value(USUARIO_EMAIL))
				.andExpect(jsonPath("$.rol").value("USUARIO"))
				.andExpect(jsonPath("$.password").doesNotExist());

		mvc.perform(get("/api/admin/me").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

		mvc.perform(post("/api/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent());
		assertThat(session.isInvalid()).isTrue();
		mvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioNoPuedeConsultarEstadisticas() throws Exception {
		mvc.perform(get("/api/admin/estadisticas")
						.with(user(USUARIO_EMAIL).roles("USUARIO"))
						.param("desde", "2026-09-01")
						.param("hasta", "2026-09-02"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	@Test
	void adminConservaPerfilCompatibleYTodosLosPermisos() throws Exception {
		MockHttpSession session = iniciarSesion(ADMIN_EMAIL);

		mvc.perform(get("/api/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Administrador de prueba"))
				.andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
				.andExpect(jsonPath("$.rol").value("ADMIN"));
		mvc.perform(get("/api/admin/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Administrador de prueba"))
				.andExpect(jsonPath("$.rol").value("ADMIN"));

		mvc.perform(get("/api/admin/estadisticas")
						.session(session)
						.param("desde", "2026-09-01")
						.param("hasta", "2026-09-02"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent());
		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void credencialesInvalidasDevuelven401() throws Exception {
		mvc.perform(post("/api/login")
						.with(csrf())
						.param("username", USUARIO_EMAIL)
						.param("password", "incorrecta"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
	}

	@Test
	void csrfSigueSiendoObligatorioParaCerrarSesion() throws Exception {
		mvc.perform(post("/api/logout").with(user(ADMIN_EMAIL).roles("ADMIN")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	private MockHttpSession iniciarSesion(String email) throws Exception {
		var result = mvc.perform(post("/api/login")
						.with(csrf())
						.param("username", email)
						.param("password", PASSWORD))
				.andExpect(status().isNoContent())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}
}
