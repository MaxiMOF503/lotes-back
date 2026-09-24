package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.entity.DepartamentoEntity;
import ies.belgrano.lotes.entity.LoteEntity;
import ies.belgrano.lotes.repository.LoteRepository;
import ies.belgrano.lotes.service.LoteConsultaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicLoteController.class)
@Import({LoteConsultaService.class, ies.belgrano.lotes.config.SecurityConfig.class})
class PublicLoteContractTest {
    @MockitoBean private org.springframework.security.core.userdetails.UserDetailsService autenticacion;
    @MockitoBean private ies.belgrano.lotes.service.EstadisticaService estadisticas;


    private static final String PATH = "/api/public/v1/lotes/ficha";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LoteRepository repository;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void construyeFichaPublicaConServicioReal(boolean coordenadas) throws Exception {
        var request = get(PATH);
        if (coordenadas) {
            given(repository.findByCoordenadasExactas(new BigDecimal("-32.8895"), new BigDecimal("-68.8458")))
                    .willReturn(Optional.of(lote()));
            request.param("latitud", "-32.8895").param("longitud", "-68.8458");
        } else {
            given(repository.findByIdentificador("LOT-DEMO-001")).willReturn(Optional.of(lote()));
            request.param("identificador", " LOT-DEMO-001 ");
        }
        ResultActions result = mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$.criterioConsulta.tipo").value(coordenadas ? "COORDENADAS" : "IDENTIFICADOR"))
                .andExpect(jsonPath("$.lote.identificador").value("LOT-DEMO-001"))
                .andExpect(jsonPath("$.lote.direccionAproximada").value("Calle Demostración 123, Mendoza"))
                .andExpect(jsonPath("$.lote.departamento").value("Luján de Cuyo"))
                .andExpect(jsonPath("$.lote.coordenadas.latitud").value(-32.8895))
                .andExpect(jsonPath("$.lote.coordenadas.longitud").value(-68.8458))
                .andExpect(jsonPath("$.zonificacion.descripcion").value("Residencial R2 (dato simulado)"))
                .andExpect(jsonPath("$.zonificacion.verificada").value(false))
                .andExpect(jsonPath("$.infraestructuraElectrica.distanciaRedElectricaMts").value(320.0))
                .andExpect(jsonPath("$.infraestructuraElectrica.tieneAcceso").value(true))
                .andExpect(jsonPath("$.coberturaAgua.tieneCobertura").value(true))
                .andExpect(jsonPath("$.advertencia").value(LoteConsultaService.ADVERTENCIA))
                .andExpect(jsonPath("$.dondeConsultar.departamento").value("Luján de Cuyo"))
                .andExpect(jsonPath("$.dondeConsultar.contacto").value("Contacto demostrativo no oficial"))
                .andExpect(jsonPath("$.dondeConsultar.direccionOficina").value("Oficina demostrativa"))
                .andExpect(jsonPath("$.dondeConsultar.comoConsultar").value("Consultar ante el organismo municipal competente"))
                .andExpect(jsonPath("$.lote.id").doesNotExist())
                .andExpect(jsonPath("$.lote.titularSimulado").doesNotExist());
        for (String section : new String[]{"lote", "zonificacion", "infraestructuraElectrica", "coberturaAgua", "dondeConsultar"}) {
            result.andExpect(jsonPath("$." + section + ".procedencia.tipo").value("SIMULADO"))
                    .andExpect(jsonPath("$." + section + ".procedencia.fuente").value("Dataset demostrativo B-01"))
                    .andExpect(jsonPath("$." + section + ".procedencia.oficial").value(false));
        }
        if (coordenadas) {
            result.andExpect(jsonPath("$.criterioConsulta.identificador").isEmpty())
                    .andExpect(jsonPath("$.criterioConsulta.latitud").value(-32.8895))
                    .andExpect(jsonPath("$.criterioConsulta.longitud").value(-68.8458));
            verify(repository).findByCoordenadasExactas(new BigDecimal("-32.8895"), new BigDecimal("-68.8458"));
        } else {
            result.andExpect(jsonPath("$.criterioConsulta.identificador").value("LOT-DEMO-001"))
                    .andExpect(jsonPath("$.criterioConsulta.latitud").isEmpty())
                    .andExpect(jsonPath("$.criterioConsulta.longitud").isEmpty());
            verify(repository).findByIdentificador("LOT-DEMO-001");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?identificador=", "?identificador=   ",
            "?latitud=-32.8895", "?longitud=-68.8458", "?latitud=&longitud=",
            "?latitud=91&longitud=0", "?latitud=-91&longitud=0",
            "?latitud=0&longitud=181", "?latitud=0&longitud=-181",
            "?latitud=abc&longitud=0", "?latitud=0&longitud=abc",
            "?identificador=LOT-DEMO-001&latitud=0&longitud=0",
            "?identificador=LOT-DEMO-001&latitud=0", "?identificador=LOT-DEMO-001&longitud=0",
            "?identificador=&latitud=0&longitud=0"})
    void rechazaConsultaInvalidaAntesDeBuscar(String query) throws Exception {
        verificarError(mvc.perform(get(PATH + query)), 400, "CONSULTA_INVALIDA")
                .andExpect(jsonPath("$.errores").isNotEmpty())
                .andExpect(jsonPath("$.errores[0].campo").isNotEmpty())
                .andExpect(jsonPath("$.errores[0].mensaje").isNotEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void rechazaIdentificadorDemasiadoLargo() throws Exception {
        verificarError(mvc.perform(get(PATH).param("identificador", "X".repeat(101))), 400, "CONSULTA_INVALIDA")
                .andExpect(jsonPath("$.errores[?(@.campo == 'identificador')]").exists());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"?identificador=NO-EXISTE", "?latitud=0&longitud=0",
            "?latitud=-90&longitud=-180", "?latitud=90&longitud=180"})
    void criterioValidoSinResultadoDevuelve404(String query) throws Exception {
        verificarError(mvc.perform(get(PATH + query)), 404, "LOTE_NO_ENCONTRADO")
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void fallaDePersistenciaDevuelve500SinFiltrarDetallesInternos() throws Exception {
        given(repository.findByIdentificador("LOT-DEMO-001"))
                .willThrow(new DataAccessResourceFailureException("detalle interno de conexión"));
        verificarError(mvc.perform(get(PATH).param("identificador", "LOT-DEMO-001")), 500, "ERROR_INTERNO")
                .andExpect(jsonPath("$.mensaje").value("No se pudo completar la consulta"))
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void loteConDatosParcialesConservaNullSinInventarInformacion() throws Exception {
        var geometry = new GeometryFactory(new PrecisionModel(), 4326);
        given(repository.findByIdentificador("PARCIAL")).willReturn(Optional.of(new LoteEntity(
                "PARCIAL", geometry.createPoint(new Coordinate(-68.8458, -32.8895)),
                null, null, null, false, null, false, false, null, null, true)));

        mvc.perform(get(PATH).param("identificador", "PARCIAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lote.direccionAproximada").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.lote.departamento").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.zonificacion.descripcion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.infraestructuraElectrica.distanciaRedElectricaMts").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.dondeConsultar").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.lote.procedencia.oficial").value(false))
                .andExpect(jsonPath("$.advertencia").value(LoteConsultaService.ADVERTENCIA));
    }

    @Test
    void departamentoSinContactoNoRecibeDatosInventados() throws Exception {
        var geometry = new GeometryFactory(new PrecisionModel(), 4326);
        given(repository.findByIdentificador("PARCIAL")).willReturn(Optional.of(new LoteEntity(
                "PARCIAL", geometry.createPoint(new Coordinate(-68.8458, -32.8895)),
                new DepartamentoEntity("Departamento de prueba", null, null, null, null),
                null, null, false, null, false, false, null, null, true)));

        mvc.perform(get(PATH).param("identificador", "PARCIAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dondeConsultar.departamento").value("Departamento de prueba"))
                .andExpect(jsonPath("$.dondeConsultar.contacto").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.dondeConsultar.direccionOficina").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.dondeConsultar.comoConsultar").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.dondeConsultar.procedencia.fuente").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.dondeConsultar.procedencia.oficial").value(false));
    }

    private ResultActions verificarError(ResultActions result, int status, String codigo) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.path").value(PATH))
                .andExpect(jsonPath("$.errores").isArray());
    }

    private LoteEntity lote() {
        var geometry = new GeometryFactory(new PrecisionModel(), 4326);
        return new LoteEntity("LOT-DEMO-001", geometry.createPoint(new Coordinate(-68.8458, -32.8895)),
                new DepartamentoEntity("Luján de Cuyo", "Contacto demostrativo no oficial", "Oficina demostrativa", "Consultar ante el organismo municipal competente", "Dataset demostrativo B-01"),
                "Calle Demostración 123, Mendoza", "Residencial R2 (dato simulado)", false,
                320.0, true, true, "Titular demostrativo no oficial", "Situación dominial simulada sin valor legal", true);
    }
}
