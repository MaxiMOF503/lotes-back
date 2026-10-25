package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.config.SecurityConfig;
import ies.belgrano.lotes.dto.response.CalidadCatastroResponse.*;
import ies.belgrano.lotes.service.CalidadCatastroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalidadCatastroController.class) @Import(SecurityConfig.class)
class CalidadCatastroControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CalidadCatastroService service;
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService autenticacion;
    @MockitoBean ies.belgrano.lotes.service.EstadisticaService estadisticas;
    static final String URL="/api/admin/territorio/calidad";
    @Test void anonimoNoAccede() throws Exception {
        for(String ruta:List.of(URL,URL+"/parcelas/a",URL+"/coincidencias"))mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void usuarioNoAccede() throws Exception {
        for(String ruta:List.of(URL,URL+"/parcelas/a",URL+"/coincidencias"))mvc.perform(get(ruta).with(user("ana").roles("USUARIO"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void adminInspeccionaResumenYGrupo() throws Exception {
        when(service.resumen()).thenReturn(new Resumen(1,0,1,1,1,3,3,0,true));
        when(service.grupo("b")).thenReturn(new Grupo("b","a","COINCIDENCIA_CON_CONFLICTO",List.of("ochava"),List.of()));
        when(service.grupos(true,0)).thenReturn(new PaginaGrupos(List.of(new GrupoResumen("a",2,1,0)),0,false));
        mvc.perform(get(URL).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.gruposConConflicto").value(1));
        mvc.perform(get(URL+"/parcelas/b").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.representante").value("a"));
        mvc.perform(get(URL+"/coincidencias").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].representante").value("a"));
    }
}
