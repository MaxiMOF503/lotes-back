package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.config.SecurityConfig;
import ies.belgrano.lotes.service.TerritorioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TerritorioController.class)
@Import(SecurityConfig.class)
class TerritorioControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean TerritorioService service;
    @MockitoBean ies.belgrano.lotes.service.DireccionService direcciones;
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService autenticacion;
    @MockitoBean ies.belgrano.lotes.service.EstadisticaService estadisticas;

    @Test void fuentesSonPublicasSinLogin() throws Exception {
        when(service.fuentes()).thenReturn(List.of());
        mvc.perform(get("/api/public/v1/territorio/fuentes")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verifyNoInteractions(estadisticas);
    }
    @Test void consultaTerritorialCuentaEnEstadisticasSinContarActivacionDeCapas() throws Exception {
        mvc.perform(get("/api/public/v1/territorio/consulta").param("latitud","-33").param("longitud","-68.9"))
            .andExpect(status().isOk());
        verify(estadisticas).registrar("CONSULTA","COORDENADAS","EXITO");
    }

    @Test void parametrosAusentesOMalFormadosDevuelven400() throws Exception {
        mvc.perform(get("/api/public/v1/territorio/consulta").param("latitud","-33")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/public/v1/territorio/consulta").param("latitud","texto").param("longitud","-68.9")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void direccionesYParcelasCercanasSonPublicasYNoCuentanComoConsultas() throws Exception {
        mvc.perform(get("/api/public/v1/territorio/direcciones").param("texto","San Martin 1500")).andExpect(status().isOk());
        verify(direcciones).buscar("San Martin 1500");
        mvc.perform(get("/api/public/v1/territorio/parcelas/cercanas").param("latitud","-33.03").param("longitud","-68.87"))
            .andExpect(status().isOk());
        verify(service).cercanas(-33.03,-68.87,30);
        verify(service,never()).parcela("cercanas");
        verifyNoInteractions(estadisticas);
    }
    @Test void direccionesSinTextoDevuelve400() throws Exception {
        mvc.perform(get("/api/public/v1/territorio/direcciones")).andExpect(status().isBadRequest());
        verifyNoInteractions(direcciones);
    }
}
