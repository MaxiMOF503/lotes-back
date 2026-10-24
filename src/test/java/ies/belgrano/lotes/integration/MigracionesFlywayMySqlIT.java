package ies.belgrano.lotes.integration;

import ies.belgrano.lotes.LotesApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migraciones de Flyway sobre una base propia y descartable (lotes_flyway_test): se vacía al empezar cada
 * prueba. Arranca la aplicación completa con Hibernate en validate, así que también comprueba que las
 * entidades coinciden con las tablas. Usa las mismas credenciales MYSQL_IT_* que PublicLoteMySqlIT.
 */
class MigracionesFlywayMySqlIT {
    private static final String BASE = "lotes_flyway_test";
    private static final List<String> TABLAS = List.of("consultas_guardadas", "consultas_historial", "departamentos",
        "estadisticas_diarias", "fichas_parcelas", "lineas_electricas", "lotes", "roles", "territorio_elementos",
        "territorio_fuentes", "territorio_sin_geometria", "usuarios");
    /** Cómo quedaban las tablas del catastro en una base importada antes de detectar parcelas repetidas. */
    private static final String TERRITORIO_ANTERIOR = """
        CREATE TABLE territorio_elementos (capa VARCHAR(24) NOT NULL, origen_id VARCHAR(100) NOT NULL,
          geometria GEOMETRY NOT NULL SRID 4326, propiedades JSON NOT NULL, valida BOOLEAN NOT NULL DEFAULT TRUE,
          PRIMARY KEY(capa,origen_id), SPATIAL INDEX territorio_geometria(geometria)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        CREATE TABLE territorio_fuentes (capa VARCHAR(24) PRIMARY KEY, fuente VARCHAR(255) NOT NULL,
          url VARCHAR(255) NOT NULL, sha256 CHAR(64) NOT NULL, registros INT NOT NULL, invalidos INT NOT NULL,
          importado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        CREATE TABLE territorio_sin_geometria (capa VARCHAR(24) NOT NULL, origen_id VARCHAR(100) NOT NULL,
          propiedades JSON NOT NULL, PRIMARY KEY(capa,origen_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;

    private String url;
    private String usuario;
    private String clave;
    private JdbcTemplate jdbc;

    @BeforeEach void baseVacia() {
        String base = System.getenv("MYSQL_IT_URL");
        usuario = System.getenv("MYSQL_IT_USERNAME");
        if (base == null || !base.matches("jdbc:mysql://[^/]+/lotes_ficha_test(?:\\?.*)?") || usuario == null || usuario.isBlank())
            throw new IllegalStateException("Configurar MYSQL_IT_URL (base lotes_ficha_test) y MYSQL_IT_USERNAME; esta prueba usa " + BASE);
        url = base.replace("/lotes_ficha_test", "/" + BASE);
        clave = System.getenv("MYSQL_IT_PASSWORD") == null ? "" : System.getenv("MYSQL_IT_PASSWORD");
        var origen = new DriverManagerDataSource(url, usuario, clave);
        jdbc = new JdbcTemplate(origen);
        // Solo sobre la base descartable: Flyway.clean elimina todo su contenido.
        Flyway.configure().dataSource(origen).cleanDisabled(false).load().clean();
    }

    @Test void baseNuevaQuedaConTodoElEsquemaYHibernateLoValida() {
        arrancar().close();
        assertThat(tablas()).containsAll(TABLAS);
        assertThat(versiones()).containsExactly("1:SQL", "2:SQL");
        assertThat(columnasDuplicadoDe()).isEqualTo(1);
    }

    @Test void repetirElArranqueNoVuelveAMigrarNiTocaLosDatos() {
        arrancar().close();
        jdbc.update("INSERT INTO departamentos(nombre) VALUES ('Luján de Cuyo')");
        long historial = jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Long.class);
        arrancar().close();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Long.class)).isEqualTo(historial);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM departamentos", Long.class)).isEqualTo(1);
    }

    @Test void baseQueYaTeniaLasTablasAdoptaFlywaySinPerderNingunDato() {
        // Instalación anterior: tablas creadas por Hibernate (update) y por el importador, sin Flyway.
        arrancar("--spring.flyway.enabled=false", "--spring.jpa.hibernate.ddl-auto=create").close();
        for (String sentencia : TERRITORIO_ANTERIOR.split(";\\s*\\n"))
            jdbc.execute(sentencia);
        jdbc.update("INSERT INTO usuarios(rol_id,email,nombre,password) SELECT id,'real@ejemplo.invalid','Real','hash' FROM roles WHERE nombre='USUARIO'");
        jdbc.update("INSERT INTO fichas_parcelas(actualizado_en,version,parcela_id,actualizado_por,referencia,fuente,direccion)"
            + " VALUES (NOW(6),0,'Parcelas_visor0.1','real@ejemplo.invalid','Referencia','Fuente','San Martín 1500')");
        jdbc.update("INSERT INTO territorio_elementos(capa,origen_id,geometria,propiedades)"
            + " VALUES ('catastro','Parcelas_visor0.1',ST_GeomFromText('POLYGON((0 0,1 0,1 1,0 0))',4326),'{\"nomenclatu\":\"0601\"}')");
        assertThat(tablas()).doesNotContain("flyway_schema_history");
        assertThat(columnasDuplicadoDe()).isZero();

        arrancar().close();

        assertThat(versiones()).containsExactly("0:BASELINE", "1:SQL", "2:SQL");
        assertThat(columnasDuplicadoDe()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE email='real@ejemplo.invalid'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT direccion FROM fichas_parcelas", String.class)).isEqualTo("San Martín 1500");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM territorio_elementos", Long.class)).isEqualTo(1);
    }

    @Test void importadorPrimeroYBackendDespuesCompletaElEsquema() {
        // iniciar-local.cmd prepara las tablas territoriales antes de arrancar el backend por primera vez.
        for (String sentencia : TERRITORIO_ANTERIOR.split(";\\s*\\n"))
            jdbc.execute(sentencia);
        arrancar().close();
        assertThat(versiones()).containsExactly("0:BASELINE", "1:SQL", "2:SQL");
        assertThat(tablas()).containsAll(TABLAS);
    }

    private ConfigurableApplicationContext arrancar(String... extra) {
        var argumentos = new java.util.ArrayList<>(List.of("--spring.datasource.url=" + url,
            "--spring.datasource.username=" + usuario, "--spring.datasource.password=" + clave, "--server.port=0"));
        argumentos.addAll(List.of(extra));
        return new SpringApplicationBuilder(LotesApplication.class).run(argumentos.toArray(String[]::new));
    }

    private List<String> tablas() {
        return jdbc.queryForList("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()", String.class);
    }

    private List<String> versiones() {
        return jdbc.query("SELECT version,type FROM flyway_schema_history ORDER BY installed_rank",
            (rs, fila) -> rs.getString(1) + ":" + rs.getString(2));
    }

    private int columnasDuplicadoDe() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"
            + " AND TABLE_NAME='territorio_elementos' AND COLUMN_NAME='duplicado_de'", Integer.class);
    }
}
