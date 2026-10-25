package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.config.SecurityConfig;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.Pagina;
import ies.belgrano.lotes.dto.response.ReporteDatoResponse;
import ies.belgrano.lotes.dto.response.ReporteDatoResponse.Persona;
import ies.belgrano.lotes.entity.ReporteDato;
import ies.belgrano.lotes.service.ReporteDatoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReporteDatoController.class)
@Import(SecurityConfig.class)
class ReporteDatoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ReporteDatoService service;
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService autenticacion;
    @MockitoBean ies.belgrano.lotes.service.EstadisticaService estadisticas;

    @Test void sinSesionNoPuedeCrearNiListar() throws Exception {
        mvc.perform(get("/api/mi-espacio/reportes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("AUTENTICACION_REQUERIDA"));
        mvc.perform(post("/api/mi-espacio/reportes").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(creacion()))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void usuarioCreaYVeSusReportes() throws Exception {
        given(service.crear(eq("ana@ejemplo.invalid"), any())).willReturn(reporte(ReporteDato.Estado.PENDIENTE, null));
        given(service.propios("ana@ejemplo.invalid", 0)).willReturn(new Pagina<>(
            List.of(reporte(ReporteDato.Estado.PENDIENTE, null)), 0, 1, 1));

        mvc.perform(post("/api/mi-espacio/reportes").with(user("ana@ejemplo.invalid").roles("USUARIO")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(creacion()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mvc.perform(get("/api/mi-espacio/reportes").with(user("ana@ejemplo.invalid").roles("USUARIO")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].descripcion").value("El dato publicado no coincide."));
    }

    @Test void descripcionCortaDevuelveDetalleDeValidacion() throws Exception {
        mvc.perform(post("/api/mi-espacio/reportes").with(user("ana").roles("USUARIO")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"parcelaId":"p.1","seccion":"AGUA","tipo":"FALTANTE","descripcion":"corta"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CONSULTA_INVALIDA"))
            .andExpect(jsonPath("$.errores[?(@.campo == 'descripcion')]").exists());
        verifyNoInteractions(service);
    }

    @Test void usuarioComunNoAccedeALaBandejaNiResponde() throws Exception {
        mvc.perform(get("/api/admin/reportes").with(user("ana").roles("USUARIO")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(patch("/api/admin/reportes/1").with(user("ana").roles("USUARIO")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_REVISION\"}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void adminFiltraYResponde() throws Exception {
        given(service.administrar(ReporteDato.Estado.PENDIENTE, 0)).willReturn(new Pagina<>(List.of(), 0, 0, 0));
        given(service.actualizar(eq("admin@ejemplo.invalid"), eq(4L), any()))
            .willReturn(reporte(ReporteDato.Estado.RESUELTO, "Se verificó y corrigió el dato."));

        mvc.perform(get("/api/admin/reportes").with(user("admin@ejemplo.invalid").roles("ADMIN"))
                .param("estado", "PENDIENTE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(patch("/api/admin/reportes/4").with(user("admin@ejemplo.invalid").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"RESUELTO\",\"respuesta\":\"Se verificó y corrigió el dato.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RESUELTO"))
            .andExpect(jsonPath("$.respuesta").value("Se verificó y corrigió el dato."));
    }

    @Test void estadoDesconocidoNoLlegaAlServicio() throws Exception {
        mvc.perform(get("/api/admin/reportes").with(user("admin").roles("ADMIN"))
                .param("estado", "CUALQUIERA"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        verifyNoInteractions(service);
    }

    private String creacion() {
        return """
            {"parcelaId":"p.1","seccion":"PARCELA","tipo":"INCORRECTO",
             "descripcion":"El dato publicado no coincide."}
            """;
    }

    private ReporteDatoResponse reporte(ReporteDato.Estado estado, String respuesta) {
        var fecha = Instant.parse("2026-10-25T12:00:00Z");
        return new ReporteDatoResponse(1L, "p.1", "0604", ReporteDato.Seccion.PARCELA,
            ReporteDato.Tipo.INCORRECTO, "El dato publicado no coincide.", estado, respuesta,
            new Persona("Ana", "ana@ejemplo.invalid"), respuesta == null ? null : new Persona("Admin", "admin@ejemplo.invalid"),
            fecha, fecha, respuesta == null ? null : fecha);
    }
}
