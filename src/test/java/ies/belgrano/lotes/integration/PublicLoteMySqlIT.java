package ies.belgrano.lotes.integration;

import ies.belgrano.lotes.entity.LoteEntity;
import ies.belgrano.lotes.repository.LoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class PublicLoteMySqlIT {

    private static final String PATH = "/api/public/v1/lotes/ficha";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private LoteRepository repository;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("MYSQL_IT_URL");
        if (url == null || !url.matches("jdbc:mysql://[^/]+/lotes_ficha_test(?:\\?.*)?")) {
            throw new IllegalStateException("MYSQL_IT_URL debe apuntar a una base descartable llamada lotes_ficha_test");
        }
        String user = System.getenv("MYSQL_IT_USERNAME");
        if (user == null || user.isBlank()) {
            throw new IllegalStateException("Falta MYSQL_IT_USERNAME");
        }
        String password = System.getenv("MYSQL_IT_PASSWORD");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> password == null ? "" : password);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void usaMySqlRealYGuardaElPuntoConSrid4326() {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8.");
        assertThat(jdbc.queryForObject("SELECT ST_SRID(ubicacion) FROM lotes WHERE identificador = 'LOT-DEMO-001'", Integer.class))
                .isEqualTo(4326);
        assertThat(jdbc.queryForObject("SELECT ST_Latitude(ubicacion) FROM lotes WHERE identificador = 'LOT-DEMO-001'", Double.class))
                .isEqualTo(-32.8895);
        assertThat(jdbc.queryForObject("SELECT ST_Longitude(ubicacion) FROM lotes WHERE identificador = 'LOT-DEMO-001'", Double.class))
                .isEqualTo(-68.8458);
    }

    @ParameterizedTest
    @ValueSource(strings = {"?identificador=LOT-DEMO-001", "?latitud=-32.8895&longitud=-68.8458"})
    void consultaLaFichaPersistidaSinMocks(String query) throws Exception {
        mvc.perform(get(PATH + query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lote.identificador").value("LOT-DEMO-001"))
                .andExpect(jsonPath("$.lote.coordenadas.latitud").value(-32.8895))
                .andExpect(jsonPath("$.lote.coordenadas.longitud").value(-68.8458))
                .andExpect(jsonPath("$.dondeConsultar.contacto").value("Contacto demostrativo no oficial"))
                .andExpect(jsonPath("$.dondeConsultar.procedencia.fuente").value("Dataset demostrativo B-01"))
                .andExpect(jsonPath("$.dondeConsultar.procedencia.oficial").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"?identificador=NO-EXISTE", "?latitud=-32.8896&longitud=-68.8458",
            "?latitud=-68.8458&longitud=-32.8895", "?latitud=0&longitud=0"})
    void noConfundePuntoCercanoNiCoordenadasInvertidasConCoincidencia(String query) throws Exception {
        mvc.perform(get(PATH + query))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("LOTE_NO_ENCONTRADO"));
    }

    @Test
    @Transactional
    void devuelveDatosParcialesPersistidos() throws Exception {
        var geometry = new GeometryFactory(new PrecisionModel(), 4326);
        repository.saveAndFlush(new LoteEntity("IT-PARCIAL", geometry.createPoint(new Coordinate(-68.8, -32.8)),
                null, null, null, false, null, false, false, null, null, true));
        mvc.perform(get(PATH).param("identificador", "IT-PARCIAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lote.direccionAproximada").value(nullValue()))
                .andExpect(jsonPath("$.lote.departamento").value(nullValue()))
                .andExpect(jsonPath("$.zonificacion.descripcion").value(nullValue()))
                .andExpect(jsonPath("$.infraestructuraElectrica.distanciaRedElectricaMts").value(nullValue()))
                .andExpect(jsonPath("$.dondeConsultar").value(nullValue()));
    }
}
