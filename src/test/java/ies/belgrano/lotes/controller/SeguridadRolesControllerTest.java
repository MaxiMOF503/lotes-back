package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.config.SecurityConfig;
import ies.belgrano.lotes.dto.request.LoteAdminRequest;
import ies.belgrano.lotes.dto.request.ProcedenciaRequest;
import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.repository.DepartamentoRepository;
import ies.belgrano.lotes.repository.UsuarioRepository;
import ies.belgrano.lotes.service.EstadisticaService;
import ies.belgrano.lotes.service.LoteAdminService;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SesionController.class, AdminLoteController.class, EstadisticaController.class})
@Import({SecurityConfig.class, UsuarioAutenticacionService.class})
class SeguridadRolesControllerTest {

	private static final String PASSWORD = "PasswordSoloPruebas123";
	private static final String ADMIN_EMAIL = "admin.test@lotes.invalid";
	private static final String USUARIO_EMAIL = "usuario.test@lotes.invalid";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper mapper;

	@MockitoBean
	private UsuarioRepository usuarios;

	@MockitoBean
	private LoteAdminService loteAdminService;

	@MockitoBean
	private DepartamentoRepository departamentos;

	@MockitoBean
	private EstadisticaService estadisticaService;

	private LoteAdminRequest loteRequest;

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

		loteRequest = loteRequest();
		var detalle = new LoteAdminService.Detalle(1L, loteRequest);
		given(loteAdminService.listar(anyInt()))
				.willReturn(new LoteAdminService.Pagina(List.of(), 0, 0, 0));
		given(loteAdminService.obtener(anyLong())).willReturn(detalle);
		given(loteAdminService.crear(any(LoteAdminRequest.class))).willReturn(detalle);
		given(loteAdminService.editar(anyLong(), any(LoteAdminRequest.class))).willReturn(detalle);
		given(departamentos.findAll(any(org.springframework.data.domain.Sort.class))).willReturn(List.of());
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

		mvc.perform(get("/api/admin/lotes"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("AUTENTICACION_REQUERIDA"))
				.andExpect(jsonPath("$.path").value("/api/admin/lotes"));
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
	void usuarioPuedeLeerLotesDetalleYDepartamentos() throws Exception {
		var usuario = user(USUARIO_EMAIL).roles("USUARIO");

		mvc.perform(get("/api/admin/lotes").with(usuario))
				.andExpect(status().isOk());
		mvc.perform(get("/api/admin/lotes/1").with(usuario))
				.andExpect(status().isOk());
		mvc.perform(get("/api/admin/departamentos").with(usuario))
				.andExpect(status().isOk());
	}

	@Test
	void usuarioNoPuedeEscribirLotesNiConsultarEstadisticas() throws Exception {
		String body = mapper.writeValueAsString(loteRequest);
		var usuario = user(USUARIO_EMAIL).roles("USUARIO");

		mvc.perform(post("/api/admin/lotes")
						.with(usuario)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		mvc.perform(put("/api/admin/lotes/1")
						.with(usuario)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		mvc.perform(get("/api/admin/estadisticas")
						.with(usuario)
						.param("desde", "2026-09-01")
						.param("hasta", "2026-09-02"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	@Test
	void adminConservaPerfilCompatibleYTodosLosPermisos() throws Exception {
		MockHttpSession session = iniciarSesion(ADMIN_EMAIL);
		String body = mapper.writeValueAsString(loteRequest);

		mvc.perform(get("/api/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Administrador de prueba"))
				.andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
				.andExpect(jsonPath("$.rol").value("ADMIN"));
		mvc.perform(get("/api/admin/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Administrador de prueba"))
				.andExpect(jsonPath("$.rol").value("ADMIN"));

		mvc.perform(get("/api/admin/lotes").session(session))
				.andExpect(status().isOk());
		mvc.perform(get("/api/admin/lotes/1").session(session))
				.andExpect(status().isOk());
		mvc.perform(get("/api/admin/departamentos").session(session))
				.andExpect(status().isOk());
		mvc.perform(post("/api/admin/lotes")
						.session(session)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());
		mvc.perform(put("/api/admin/lotes/1")
						.session(session)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk());
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
	void csrfSigueSiendoObligatorioParaEscrituras() throws Exception {
		String body = mapper.writeValueAsString(loteRequest);
		var admin = user(ADMIN_EMAIL).roles("ADMIN");

		mvc.perform(post("/api/admin/lotes")
						.with(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		mvc.perform(put("/api/admin/lotes/1")
						.with(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		mvc.perform(post("/api/logout").with(admin))
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

	private LoteAdminRequest loteRequest() {
		var procedencia = new ProcedenciaRequest(
				ProcedenciaRequest.Tipo.SIMULADO,
				"Datos exclusivos de prueba",
				null);
		return new LoteAdminRequest(
				"SEGURIDAD-TEST-001",
				new BigDecimal("-32.8895"),
				new BigDecimal("-68.8458"),
				null,
				"Dirección de prueba",
				"Residencial de prueba",
				false,
				10.0,
				true,
				true,
				procedencia,
				procedencia,
				procedencia,
				procedencia);
	}
}
