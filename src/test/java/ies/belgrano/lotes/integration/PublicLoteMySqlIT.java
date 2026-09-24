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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
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

    @Autowired private tools.jackson.databind.ObjectMapper mapper;
    @Autowired private ies.belgrano.lotes.service.EstadisticaService estadisticas;
    @Autowired private ies.belgrano.lotes.repository.UsuarioRepository usuarios;
    @Autowired private ies.belgrano.lotes.repository.RolRepository roles;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder encoder;

    @Test void protegeAdministracionYExigeCsrf() throws Exception {
        mvc.perform(get("/api/admin/lotes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/estadisticas?desde=2026-09-01&hasta=2026-09-23").with(user("ciudadano").roles("USUARIO")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/lotes").with(user("admin").roles("ADMIN")).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/lotes").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test void loginRealUsaPasswordCifradaYCierraSesion() throws Exception {
        var rol=roles.findByNombre(ies.belgrano.lotes.entity.RolEntity.NombreRol.ADMIN).orElseThrow();
        usuarios.saveAndFlush(new ies.belgrano.lotes.entity.UsuarioEntity("Admin de prueba","it-admin@lotes.invalid",encoder.encode("PasswordSoloPruebas123"),rol));
        mvc.perform(post("/api/login").with(csrf()).param("username","it-admin@lotes.invalid").param("password","incorrecta"))
            .andExpect(status().isUnauthorized());
        var login=mvc.perform(post("/api/login").with(csrf()).param("username","it-admin@lotes.invalid").param("password","PasswordSoloPruebas123"))
            .andExpect(status().isNoContent()).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(get("/api/admin/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/me")).andExpect(status().isUnauthorized());
    }

    private String loteJson(String id) throws Exception {
        var p=new ies.belgrano.lotes.dto.request.ProcedenciaRequest(ies.belgrano.lotes.dto.request.ProcedenciaRequest.Tipo.OFICIAL,"Fuente municipal de prueba","Expediente de prueba 123");
        var sim=new ies.belgrano.lotes.dto.request.ProcedenciaRequest(ies.belgrano.lotes.dto.request.ProcedenciaRequest.Tipo.SIMULADO,"Simulación de prueba",null);
        return mapper.writeValueAsString(new ies.belgrano.lotes.dto.request.LoteAdminRequest(id,new java.math.BigDecimal("-32.87"),new java.math.BigDecimal("-68.81"),null,"Dirección inicial","Residencial",true,20.0,true,false,p,p,sim,sim));
    }

    @Test void creaEditaYPublicaProcedenciaPorSeccion() throws Exception {
        String body=loteJson("IT-ADMIN-001");
        var creado=mvc.perform(post("/api/admin/lotes").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isCreated()).andReturn();
        long id=mapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/admin/lotes").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("IDENTIFICADOR_DUPLICADO"));
        mvc.perform(put("/api/admin/lotes/"+id).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(body.replace("Dirección inicial","Dirección actualizada")))
            .andExpect(status().isOk());
        mvc.perform(get(PATH).param("identificador","IT-ADMIN-001"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.lote.direccionAproximada").value("Dirección actualizada"))
            .andExpect(jsonPath("$.lote.procedencia.oficial").value(true))
            .andExpect(jsonPath("$.zonificacion.procedencia.fuente").value("Fuente municipal de prueba"))
            .andExpect(jsonPath("$.infraestructuraElectrica.procedencia.tipo").value("SIMULADO"));
    }

    @Test void rechazaDatosIncompletosNegativosYOficialSinRespaldo() throws Exception {
        for(String body:java.util.List.of("{}",loteJson("INVALIDO").replace("20.0","-1"),loteJson("INVALIDO").replace("Expediente de prueba 123",""),loteJson("INVALIDO").replace("-32.87","91"))) {
            mvc.perform(post("/api/admin/lotes").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        assertThat(repository.existsByIdentificador("INVALIDO")).isFalse();
    }

    @Test void cuentaUnaVezPorConsultaYSeparadoDeVisitas() throws Exception {
        var hoy=java.time.LocalDate.now(java.time.ZoneId.of("America/Argentina/Buenos_Aires"));
        var antes=estadisticas.consultar(hoy,hoy);
        mvc.perform(get(PATH).param("identificador","LOT-DEMO-001")).andExpect(status().isOk());
        mvc.perform(get(PATH).param("identificador","NO-EXISTE")).andExpect(status().isNotFound());
        mvc.perform(get(PATH)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/public/v1/visitas").with(csrf())).andExpect(status().isNoContent());
        var despues=estadisticas.consultar(hoy,hoy);
        assertThat(despues.consultas()-antes.consultas()).isEqualTo(3);
        assertThat(despues.visitas()-antes.visitas()).isEqualTo(1);
        assertThat(despues.dias().getFirst().exitosas()-antes.dias().getFirst().exitosas()).isEqualTo(1);
        assertThat(despues.dias().getFirst().sinResultados()-antes.dias().getFirst().sinResultados()).isEqualTo(1);
        assertThat(despues.dias().getFirst().invalidas()-antes.dias().getFirst().invalidas()).isEqualTo(1);
    }

    @Test void rechazaRangosInvalidosYDevuelveDiasVacios() throws Exception {
        mvc.perform(get("/api/admin/estadisticas?desde=2026-09-23&hasta=2026-09-01").with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/estadisticas?desde=2020-01-01&hasta=2026-09-01").with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/estadisticas?desde=2020-01-01&hasta=2020-01-02").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.consultas").value(0)).andExpect(jsonPath("$.dias.length()").value(2));
    }
}
