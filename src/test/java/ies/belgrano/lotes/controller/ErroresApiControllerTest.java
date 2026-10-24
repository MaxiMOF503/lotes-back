package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.config.CorrelacionFilter;
import ies.belgrano.lotes.config.SecurityConfig;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.service.EstadisticaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Todos los errores comparten formato y traen el identificador de correlación, sin trazas ni datos internos. */
@WebMvcTest(ErroresApiControllerTest.Disparador.class)
@Import({SecurityConfig.class, CorrelacionFilter.class, ErroresApiControllerTest.Disparador.class})
class ErroresApiControllerTest {
	private static final String BASE = "/api/public/v1/prueba";

	@RestController
	static class Disparador {
		record Datos(@NotBlank String nombre) {}
		@PostMapping(BASE + "/validacion") void validar(@Valid @RequestBody Datos datos) {}
		@GetMapping(BASE + "/numero") void numero(@RequestParam int n) {}
		@GetMapping(BASE + "/no-encontrado") void noEncontrado() { throw new OperacionInvalidaException(404, "PARCELA_NO_ENCONTRADA", "No existe esa parcela"); }
		@GetMapping(BASE + "/ficha-modificada") void ficha() { throw new org.springframework.orm.ObjectOptimisticLockingFailureException("Ficha", 1L); }
		@GetMapping(BASE + "/duplicado") void duplicado() { throw new org.springframework.dao.DataIntegrityViolationException("Duplicate entry 'secreto@x' for key"); }
		@GetMapping(BASE + "/georef") void georef() { throw new OperacionInvalidaException(503, "DIRECCIONES_NO_DISPONIBLES", "El servicio oficial de direcciones no respondió."); }
		@GetMapping(BASE + "/capa") void capa() { throw new OperacionInvalidaException(503, "CAPA_NO_IMPORTADA", "La capa catastro no fue importada."); }
		@GetMapping(BASE + "/base") void base() { throw new org.springframework.jdbc.CannotGetJdbcConnectionException("jdbc:mysql://servidor-secreto:3306/base usuario=root"); }
		@GetMapping(BASE + "/falla") void falla() { throw new IllegalStateException("detalle interno secreto en /srv/app/Clase.java"); }
		@GetMapping("/api/mi-espacio/prueba") void soloConSesion() {}
		@GetMapping("/api/admin/prueba") void soloAdmin() {}
	}

	@Autowired MockMvc mvc;
	@MockitoBean org.springframework.security.core.userdetails.UserDetailsService autenticacion;
	@MockitoBean EstadisticaService estadisticas;

	private ResultActions formato(ResultActions r, int status, String codigo) throws Exception {
		return r.andExpect(status().is(status))
			.andExpect(jsonPath("$.status").value(status))
			.andExpect(jsonPath("$.codigo").value(codigo))
			.andExpect(jsonPath("$.mensaje", not(emptyOrNullString())))
			.andExpect(jsonPath("$.timestamp", not(emptyOrNullString())))
			.andExpect(jsonPath("$.path", startsWith("/api/")))
			.andExpect(jsonPath("$.correlacionId", not(emptyOrNullString())))
			.andExpect(header().string("X-Correlation-Id", not(emptyOrNullString())))
			.andExpect(content().string(not(containsString("secreto"))))
			.andExpect(content().string(not(containsString("at ies."))))
			.andExpect(content().string(not(containsString(".java"))));
	}

	@Test void error400PorValidacionListaLosCampos() throws Exception {
		formato(mvc.perform(post(BASE + "/validacion").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}")),
			400, "CONSULTA_INVALIDA").andExpect(jsonPath("$.errores[0].campo").value("nombre"));
	}
	@Test void error400PorParametroMalFormadoOAusente() throws Exception {
		formato(mvc.perform(get(BASE + "/numero").param("n", "abc")), 400, "DATOS_INVALIDOS");
		formato(mvc.perform(get(BASE + "/numero")), 400, "DATOS_INVALIDOS");
		formato(mvc.perform(post(BASE + "/validacion").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("no es json")), 400, "DATOS_INVALIDOS");
	}
	@Test void error401SinSesion() throws Exception {
		formato(mvc.perform(get("/api/mi-espacio/prueba")), 401, "AUTENTICACION_REQUERIDA");
	}
	@Test void error403PorPermisoInsuficienteYPorCsrfSeDistinguen() throws Exception {
		formato(mvc.perform(get("/api/admin/prueba").with(user("ana").roles("USUARIO"))), 403, "ACCESO_DENEGADO");
		formato(mvc.perform(post(BASE + "/validacion").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"a\"}")), 403, "CSRF_INVALIDO");
	}
	@Test void error404ParaDatoInexistenteYRutaInexistente() throws Exception {
		formato(mvc.perform(get(BASE + "/no-encontrado")), 404, "PARCELA_NO_ENCONTRADA");
		formato(mvc.perform(get("/api/public/v1/no-existe")), 404, "RECURSO_NO_ENCONTRADO");
	}
	@Test void error405y415() throws Exception {
		formato(mvc.perform(get(BASE + "/validacion")), 405, "METODO_NO_PERMITIDO");
		formato(mvc.perform(post(BASE + "/validacion").with(csrf()).contentType(MediaType.TEXT_PLAIN).content("x")), 415, "FORMATO_NO_SOPORTADO");
	}
	@Test void error409PorConflictoYPorFichaModificada() throws Exception {
		formato(mvc.perform(get(BASE + "/duplicado")), 409, "CONFLICTO_DATOS");
		formato(mvc.perform(get(BASE + "/ficha-modificada")), 409, "FICHA_MODIFICADA");
	}
	@Test void error503PorDependenciaExternaCapaNoImportadaYBase() throws Exception {
		formato(mvc.perform(get(BASE + "/georef")), 503, "DIRECCIONES_NO_DISPONIBLES");
		formato(mvc.perform(get(BASE + "/capa")), 503, "CAPA_NO_IMPORTADA");
		formato(mvc.perform(get(BASE + "/base")), 503, "BASE_DATOS_NO_DISPONIBLE");
	}
	@Test void error500NoExponeElMensajeNiLaTraza() throws Exception {
		formato(mvc.perform(get(BASE + "/falla")), 500, "ERROR_INTERNO");
	}
	@Test void usaElIdentificadorEntranteSoloSiTieneUnFormatoInocuo() throws Exception {
		mvc.perform(get(BASE + "/falla").header("X-Correlation-Id", "abc12345-prueba"))
			.andExpect(jsonPath("$.correlacionId").value("abc12345-prueba"))
			.andExpect(header().string("X-Correlation-Id", "abc12345-prueba"));
		mvc.perform(get(BASE + "/falla").header("X-Correlation-Id", "<script>alert(1)</script>"))
			.andExpect(jsonPath("$.correlacionId", not(containsString("<"))));
	}
}
