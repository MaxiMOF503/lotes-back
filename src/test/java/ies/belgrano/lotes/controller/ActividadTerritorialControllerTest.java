package ies.belgrano.lotes.controller;
import ies.belgrano.lotes.config.SecurityConfig;
import ies.belgrano.lotes.service.ActividadTerritorialService;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.Pagina;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActividadTerritorialController.class) @Import(SecurityConfig.class)
class ActividadTerritorialControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean ActividadTerritorialService service;
 @MockitoBean org.springframework.security.core.userdetails.UserDetailsService autenticacion;
 @MockitoBean ies.belgrano.lotes.service.EstadisticaService estadisticas;
 @Test void lecturasPrivadasRequierenSesion() throws Exception {
   for(String ruta:List.of("/historial","/historial/1/informe","/guardadas/1/informe","/comparacion?ids=1,2"))
     mvc.perform(get("/api/mi-espacio"+ruta)).andExpect(status().isUnauthorized());
   verifyNoInteractions(service);
 }
 @Test void historialUsaCuentaDelPrincipal() throws Exception {
   when(service.historial("ana",0)).thenReturn(new Pagina<>(List.of(),0,0,0));
   mvc.perform(get("/api/mi-espacio/historial?email=bea").with(user("ana").roles("USUARIO")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
   verify(service).historial("ana",0);
 }
 @Test void consultaPersonalExigeCsrf() throws Exception {
   mvc.perform(post("/api/mi-espacio/consultas").with(user("ana").roles("USUARIO"))
     .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-33,\"longitud\":-68.8}")).andExpect(status().isForbidden());
   verifyNoInteractions(service);
 }
 @Test void consultaConCsrfYCoordenadasUsaPrincipal() throws Exception {
   mvc.perform(post("/api/mi-espacio/consultas").with(user("ana").roles("USUARIO")).with(csrf())
     .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-33,\"longitud\":-68.8}")).andExpect(status().isOk());
   verify(service).consultar("ana",-33,-68.8);
 }
 @Test void consultaSinCoordenadasNoSeRegistra() throws Exception {
   mvc.perform(post("/api/mi-espacio/consultas").with(user("ana").roles("USUARIO")).with(csrf())
     .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
   verifyNoInteractions(service);
 }
 @Test void adminTambienPuedeCompararSoloSuCuenta() throws Exception {
   when(service.comparar("admin",List.of(1L,2L))).thenReturn(List.of());
   mvc.perform(get("/api/mi-espacio/comparacion?ids=1,2").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
   verify(service).comparar("admin",List.of(1L,2L));
 }
 @Test void quitarHistorialExigeCsrfYPropietario() throws Exception {
   mvc.perform(delete("/api/mi-espacio/historial/4").with(user("ana").roles("USUARIO"))).andExpect(status().isForbidden());
   mvc.perform(delete("/api/mi-espacio/historial/4").with(user("ana").roles("USUARIO")).with(csrf())).andExpect(status().isNoContent());
   verify(service).quitarHistorial("ana",4L);
 }
}
