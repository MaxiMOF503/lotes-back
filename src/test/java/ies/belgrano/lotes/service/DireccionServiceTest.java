package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.DireccionResponse.*;
import ies.belgrano.lotes.dto.response.TerritorioResponse.Feature;
import ies.belgrano.lotes.entity.FichaParcela;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.FichaParcelaRepository;
import ies.belgrano.lotes.repository.TerritorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Georef se simula con respuestas reales de Luján de Cuyo: los tests no dependen de internet. */
class DireccionServiceTest {
    private static final String CALLE_Y_NUMERO = """
        {"cantidad":1,"direcciones":[{"altura":{"unidad":null,"valor":1500},"calle":{"categoria":"AV","id":"5006307000190","nombre":"AV GRL J DE SAN MARTIN"},"calle_cruce_1":{"categoria":null,"id":null,"nombre":null},"calle_cruce_2":{"categoria":null,"id":null,"nombre":null},"departamento":{"id":"50063","nombre":"Luján de Cuyo"},"localidad_censal":{"id":"50063070","nombre":"Las Compuertas"},"nomenclatura":"AV GRL J DE SAN MARTIN 1500, Luján de Cuyo, Mendoza","piso":null,"provincia":{"id":"50","nombre":"Mendoza"},"ubicacion":{"lat":-33.0262588845649,"lon":-68.8749132362673}}],"inicio":0,"total":1}
        """;
    private static final String ESQUINA = """
        {"cantidad":1,"direcciones":[{"altura":{"unidad":null,"valor":null},"calle":{"categoria":"AV","id":"5006307000190","nombre":"AV GRL J DE SAN MARTIN"},"calle_cruce_1":{"categoria":"CALLE","id":"5006307005170","nombre":"PRES ROQUE SAENZ PENA"},"calle_cruce_2":{"categoria":null,"id":null,"nombre":null},"departamento":{"id":"50063","nombre":"Luján de Cuyo"},"localidad_censal":{"id":"50063070","nombre":"Las Compuertas"},"nomenclatura":"AV GRL J DE SAN MARTIN (ESQUINA PRES ROQUE SAENZ PENA), Luján de Cuyo, Mendoza","piso":null,"provincia":{"id":"50","nombre":"Mendoza"},"ubicacion":{"lat":-33.0385356569091,"lon":-68.8800290274383}}],"inicio":0,"total":1}
        """;
    private static final String SIN_UBICACION = """
        {"cantidad":1,"direcciones":[{"altura":{"valor":99999},"calle":{"nombre":"CALLE SIN TRAMO"},"nomenclatura":"CALLE SIN TRAMO 99999","ubicacion":{"lat":null,"lon":null}}]}
        """;

    private final FichaParcelaRepository fichas = mock(FichaParcelaRepository.class);
    private final TerritorioRepository territorio = mock(TerritorioRepository.class);
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private MockRestServiceServer georef;
    private DireccionService service;

    @BeforeEach void configurar() {
        var builder = RestClient.builder().baseUrl("https://georef.test/api");
        georef = MockRestServiceServer.bindTo(builder).build();
        service = new DireccionService(builder.build(), fichas, territorio, mapper);
    }

    @Test void calleYNumeroDevuelveLaUbicacionAproximadaLimitadaALujanDeCuyo() {
        georef.expect(requestTo(containsString("/direcciones?direccion=San%20Martin%201500")))
            .andExpect(queryParam("provincia", "Mendoza"))
            .andExpect(queryParam("departamento", "Lujan%20de%20Cuyo"))
            .andExpect(queryParam("max", "5"))
            .andRespond(withSuccess(CALLE_Y_NUMERO, MediaType.APPLICATION_JSON));
        var resultado = service.buscar("  San   Martin 1500 ");
        georef.verify();
        assertThat(resultado.resultados()).singleElement().satisfies(d -> {
            assertThat(d.origen()).isEqualTo(Origen.GEOREF);
            assertThat(d.calle()).isEqualTo("AV GRL J DE SAN MARTIN");
            assertThat(d.altura()).isEqualTo(1500);
            assertThat(d.cruce()).isNull();
            assertThat(d.localidad()).isEqualTo("Las Compuertas");
            assertThat(d.latitud()).isEqualTo(-33.0262588845649);
            assertThat(d.longitud()).isEqualTo(-68.8749132362673);
        });
        assertThat(resultado.advertencia()).contains("aproximada");
    }

    @Test void esquinaDevuelveLaCalleQueCruzaSinAltura() {
        georef.expect(requestTo(containsString("direccion=San%20Martin%20esquina%20Saenz%20Pe%C3%B1a")))
            .andRespond(withSuccess(ESQUINA, MediaType.APPLICATION_JSON));
        var d = service.buscar("San Martin esquina Saenz Peña").resultados().getFirst();
        assertThat(d.cruce()).isEqualTo("PRES ROQUE SAENZ PENA");
        assertThat(d.altura()).isNull();
        assertThat(d.texto()).contains("ESQUINA");
    }

    @Test void resultadoSinCoordenadasSeOmitePorqueNoSePuedeUbicar() {
        georef.expect(requestTo(containsString("/direcciones"))).andRespond(withSuccess(SIN_UBICACION, MediaType.APPLICATION_JSON));
        assertThat(service.buscar("Calle sin tramo 99999").resultados()).isEmpty();
    }

    @Test void georefCaidoSinDireccionesRelevadasDevuelve503ClaroSinExponerElTexto() {
        georef.expect(requestTo(containsString("/direcciones"))).andRespond(withServerError());
        assertThatThrownBy(() -> service.buscar("San Martin 1500"))
            .isInstanceOfSatisfying(OperacionInvalidaException.class, e -> {
                assertThat(e.status).isEqualTo(503);
                assertThat(e.codigo).isEqualTo("DIRECCIONES_NO_DISPONIBLES");
                assertThat(e.getCause()).isNull();
                assertThat(e.getMessage()).doesNotContain("San Martin");
            });
    }

    @Test void georefCaidoConDireccionRelevadaDevuelveLaRelevadaConAviso() {
        relevada("Parcelas_visor0.1", "San Martín 1500, Las Compuertas", "060102000200002200001");
        georef.expect(requestTo(containsString("/direcciones"))).andRespond(withServerError());
        var resultado = service.buscar("San Martín 1500");
        assertThat(resultado.resultados()).singleElement().extracting(Direccion::origen).isEqualTo(Origen.FICHA);
        assertThat(resultado.advertencia()).contains("no respondió");
    }

    @Test void direccionRelevadaPorElEquipoVaPrimeroConSuParcela() {
        relevada("Parcelas_visor0.1", "San Martín 1500, Las Compuertas", "060102000200002200001");
        georef.expect(requestTo(containsString("/direcciones"))).andRespond(withSuccess(CALLE_Y_NUMERO, MediaType.APPLICATION_JSON));
        var resultados = service.buscar("San Martín 1500").resultados();
        assertThat(resultados).extracting(Direccion::origen).containsExactly(Origen.FICHA, Origen.GEOREF);
        assertThat(resultados.getFirst().parcelaId()).isEqualTo("Parcelas_visor0.1");
        assertThat(resultados.getFirst().nomenclatura()).isEqualTo("060102000200002200001");
    }

    @Test void textoMuyCortoOMuyLargoSeRechazaSinConsultarGeoref() {
        assertThatThrownBy(() -> service.buscar(" ab ")).isInstanceOfSatisfying(OperacionInvalidaException.class,
            e -> assertThat(e.codigo).isEqualTo("DIRECCION_INVALIDA"));
        assertThatThrownBy(() -> service.buscar("x".repeat(151))).isInstanceOf(OperacionInvalidaException.class);
        georef.verify();
        verifyNoInteractions(fichas, territorio);
    }

    private void relevada(String parcelaId, String direccion, String nomenclatura) {
        var ficha = new FichaParcela(parcelaId);
        ficha.actualizar(direccion, null, "Relevamiento del equipo", "Visita al lugar", "admin@test");
        when(fichas.findTop5ByDireccionContainingIgnoreCaseOrderByDireccion(anyString())).thenReturn(List.of(ficha));
        when(territorio.parcela(parcelaId)).thenReturn(Optional.of(new Feature("Feature", parcelaId,
            mapper.readTree("{}"), mapper.readTree("{\"nomenclatu\":\"" + nomenclatura + "\"}"))));
    }
}
