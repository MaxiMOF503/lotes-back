package ies.belgrano.lotes.controller;
import ies.belgrano.lotes.config.SecurityConfig;
import ies.belgrano.lotes.service.EspacioTerritorialService;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EspacioTerritorialController.class) @Import(SecurityConfig.class)
class EspacioTerritorialControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean EspacioTerritorialService service;
 @MockitoBean org.springframework.security.core.userdetails.UserDetailsService autenticacion;
 @MockitoBean ies.belgrano.lotes.service.EstadisticaService estadisticas;
 private static final String GUARDAR="{\"latitud\":-33.0688,\"longitud\":-68.8929}";
 private static final String FICHA="{\"fuente\":\"Relevamiento\",\"referencia\":\"Acta 1\"}";
 @Test void guardadasRequierenLogin() throws Exception {
   mvc.perform(get("/api/mi-espacio/guardadas")).andExpect(status().isUnauthorized());
   verifyNoInteractions(service);
 }
 @Test void usuarioSoloObtieneSuListaSegunPrincipal() throws Exception {
   when(service.guardadas("ana@ejemplo.invalid",0)).thenReturn(new Pagina<>(List.of(),0,0,0));
   mvc.perform(get("/api/mi-espacio/guardadas").with(user("ana@ejemplo.invalid").roles("USUARIO")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
   verify(service).guardadas("ana@ejemplo.invalid",0);
 }
 @Test void escrituraPersonalRequiereCsrfYUsaPrincipalIgnorandoUsuarioEnBody() throws Exception {
   mvc.perform(put("/api/mi-espacio/guardadas/123").with(user("ana").roles("USUARIO"))
     .contentType(MediaType.APPLICATION_JSON).content(GUARDAR)).andExpect(status().isForbidden());
   mvc.perform(put("/api/mi-espacio/guardadas/123").with(user("ana").roles("USUARIO")).with(csrf())
     .contentType(MediaType.APPLICATION_JSON).content(GUARDAR)).andExpect(status().isOk());
   verify(service).guardar(eq("ana"),eq("123"),any());
 }
 @Test void quitarUsaPropietarioDeSesion() throws Exception {
   mvc.perform(delete("/api/mi-espacio/guardadas/42").with(user("ana").roles("USUARIO")).with(csrf()))
     .andExpect(status().isNoContent());
   verify(service).quitar("ana",42L);
 }
 @Test void usuarioNoPuedeLeerNiEscribirGestionAdmin() throws Exception {
   mvc.perform(get("/api/admin/parcelas/123/ficha").with(user("ana").roles("USUARIO"))).andExpect(status().isForbidden());
   mvc.perform(put("/api/admin/parcelas/123/ficha").with(user("ana").roles("USUARIO")).with(csrf())
      .contentType(MediaType.APPLICATION_JSON).content(FICHA)).andExpect(status().isForbidden());
   verifyNoInteractions(service);
 }
 @Test void adminNoPuedeEscribirSinCsrf() throws Exception {
   mvc.perform(put("/api/admin/parcelas/123/ficha").with(user("admin").roles("ADMIN"))
      .contentType(MediaType.APPLICATION_JSON).content(FICHA)).andExpect(status().isForbidden());
 }
 @Test void adminNecesitaFuenteYReferencia() throws Exception {
   mvc.perform(put("/api/admin/parcelas/123/ficha").with(user("admin").roles("ADMIN")).with(csrf())
      .contentType(MediaType.APPLICATION_JSON).content("{\"fuente\":\" \",\"referencia\":\"\"}"))
      .andExpect(status().isBadRequest());
   verifyNoInteractions(service);
 }
 @Test void fichaPublicaNoExponeCorreoDeAutorNiVersion() throws Exception {
   when(service.publica("123")).thenReturn(new FichaPublica("Dirección","Observación","Fuente","Acta",Instant.now()));
   mvc.perform(get("/api/public/v1/territorio/parcelas/123/ficha"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.actualizadoPor").doesNotExist())
      .andExpect(jsonPath("$.version").doesNotExist());
 }
}
