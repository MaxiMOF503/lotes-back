package ies.belgrano.lotes.integration;

import ies.belgrano.lotes.repository.TerritorioRepository;
import ies.belgrano.lotes.service.CalidadCatastroService;
import ies.belgrano.lotes.service.TerritorioService;
import ies.belgrano.lotes.service.EspacioTerritorialService;
import ies.belgrano.lotes.service.ActividadTerritorialService;
import ies.belgrano.lotes.service.ReporteDatoService;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.repository.*;
import ies.belgrano.lotes.dto.request.EspacioTerritorialRequest.*;
import ies.belgrano.lotes.dto.request.ReporteDatoRequest.Crear;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

/** Ejecuta el clasificador Python real y su SQL en MySQL. Fixtures con rollback. */
@SpringBootTest(properties={"ADMIN_EMAIL=","ADMIN_PASSWORD=","USUARIO_EMAIL=","USUARIO_PASSWORD="}) @Transactional
@EnabledIfEnvironmentVariable(named="ISSUE49_MYSQL_IT",matches="true")
@ContextConfiguration(initializers=ClasificacionDuplicadosMySqlIT.BaseAislada.class)
class ClasificacionDuplicadosMySqlIT {
    /** Se valida ANTES de arrancar Flyway/Hibernate: nunca usa una base de otra tarea. */
    public static class BaseAislada implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext context){
            String url=context.getEnvironment().getProperty("spring.datasource.url","");
            if(!url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost)(?::[0-9]+)?/lotes_issue49_test(?:\\?.*)?"))
                throw new IllegalStateException("#49 requiere MySQL local y la base exclusiva lotes_issue49_test.");
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired CalidadCatastroService calidad;
    @Autowired TerritorioRepository territorio;
    @Autowired TerritorioService consultas;
    @Autowired EspacioTerritorialService espacio;
    @Autowired ActividadTerritorialService actividad;
    @Autowired ReporteDatoService reportes;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired ConsultaGuardadaRepository guardadas;
    @Autowired ConsultaHistorialRepository historial;
    final JsonMapper mapper=JsonMapper.builder().build();
    String prefijo,nomenclatura,sql;
    List<List<Object>> filas;
    static final String GEOMETRIA="""
        {"type":"MultiPolygon","coordinates":[[[[-68.846,-32.890],[-68.844,-32.890],[-68.844,-32.888],[-68.846,-32.890]]]]}
        """;
    @BeforeEach void preparar() throws Exception {
        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success=1",String.class)).contains("5");
        for(String capa:List.of("catastro","aysam","electricidad")){
            // Fuentes sintéticas sólo para activar los servicios sobre estos fixtures.
            // Si la base ya tiene fuentes, se conservan; los inserts nuevos tienen rollback.
            jdbc.update("INSERT INTO territorio_fuentes(capa,fuente,url,sha256,registros,invalidos) "
                +"SELECT ?,'Fixture #49','https://example.invalid',?,0,0 WHERE NOT EXISTS "
                +"(SELECT 1 FROM territorio_fuentes WHERE capa=?)",capa,"0".repeat(64),capa);
        }
        prefijo="000-issue49-"+UUID.randomUUID()+".";
        nomenclatura="99"+UUID.randomUUID().toString().replaceAll("[^0-9]","");
        var geometria=mapper.readValue(GEOMETRIA,Map.class);
        filas=new ArrayList<>();
        for(int i=0;i<4;i++){
            var propiedades=new LinkedHashMap<String,Object>();
            propiedades.put("nomenclatu",nomenclatura);propiedades.put("fid",i);
            propiedades.put("ochava",i==2?"SI":null);
            if(i==3)propiedades.put("ret_front2","");
            String id=prefijo+(char)('a'+i);
            filas.add(List.of(id,geometria,propiedades));
            jdbc.update("INSERT INTO territorio_elementos(capa,origen_id,geometria,propiedades) VALUES ('catastro',?,ST_GeomFromGeoJSON(?,1,4326),?)",
                id,GEOMETRIA,mapper.writeValueAsString(propiedades));
        }
        // Sin credenciales en argumentos/subproceso: Python únicamente genera SQL.
        var comando=new ArrayList<String>();
        if(System.getProperty("os.name").startsWith("Windows"))comando.addAll(List.of("py","-3"));
        else comando.add("python3");
        comando.addAll(List.of("-c","import sys,json;sys.path.insert(0,'scripts');import importar_territorio as i;print(i.sql_clasificacion(i.clasificar_coincidencias(json.load(sys.stdin))))"));
        var proceso=new ProcessBuilder(comando).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        var salida=java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try{return proceso.getInputStream().readAllBytes();}
            catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        });
        try(var entrada=proceso.getOutputStream()){entrada.write(mapper.writeValueAsString(filas).getBytes(StandardCharsets.UTF_8));}
        if(!proceso.waitFor(30,TimeUnit.SECONDS)){proceso.destroyForcibly();throw new IllegalStateException("No terminó el clasificador Python");}
        assertThat(proceso.exitValue()).isZero();
        sql=new String(salida.get(5,TimeUnit.SECONDS),StandardCharsets.UTF_8);
        aplicar();
    }
    void aplicar(){
        // La transacción de Spring controla los fixtures. No ejecutar COMMIT del CLI.
        for(String sentencia:sql.split(";")){
            String s=sentencia.trim();
            if(!s.isEmpty()&&!s.equals("START TRANSACTION")&&!s.equals("COMMIT"))jdbc.execute(s);
        }
    }
    @Test void persisteIdenticoYConflictosSinAlterarPropiedadesNiFilas(){
        var grupo=calidad.grupo(prefijo+"c");
        assertThat(grupo.representante()).isEqualTo(prefijo+"a");
        assertThat(grupo.clasificacion()).isEqualTo("COINCIDENCIA_CON_CONFLICTO");
        assertThat(grupo.camposConflicto()).containsExactly("ochava","ret_front2");
        assertThat(grupo.registros()).hasSize(4);
        assertThat(grupo.registros().get(1).tipoDuplicado()).isEqualTo("DUPLICADO_IDENTICO");
        assertThat(grupo.registros().get(1).camposConflicto()).isEmpty();
        assertThat(grupo.registros().get(2).tipoDuplicado()).isEqualTo("CONFLICTO_ATRIBUTOS");
        assertThat(grupo.registros().get(2).camposConflicto()).containsExactly("ochava");
        assertThat(grupo.registros().getFirst().propiedades().get("ochava").isNull()).isTrue();
        assertThat(grupo.registros().get(2).propiedades().get("ochava").asText()).isEqualTo("SI");
        assertThat(territorio.parcela(prefijo+"c")).isPresent();
        assertThat(calidad.grupos(true,0).items()).extracting(g->g.representante()).contains(prefijo+"a");
        for(String copia:List.of(prefijo+"b",prefijo+"c")){
            var referencia=consultas.resolverParcela(copia);
            assertThat(referencia.idOriginal()).isEqualTo(copia);
            assertThat(referencia.idCanonico()).isEqualTo(prefijo+"a");
            assertThat(referencia.idsRelacionados()).containsExactly(prefijo+"a",prefijo+"b",prefijo+"c",prefijo+"d");
        }
    }
    @Test void importacionRepetidaEsIdempotente(){
        var antes=calidad.grupo(prefijo+"a");aplicar();
        assertThat(calidad.grupo(prefijo+"c").registros()).isEqualTo(antes.registros());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM territorio_elementos WHERE capa='catastro' AND origen_id LIKE ?",Long.class,prefijo+"%")).isEqualTo(4);
    }
    @Test void mapaYBusquedaMuestranSoloElRepresentante(){
        assertThat(territorio.buscarParcelas(nomenclatura,20,0)).extracting(f->f.id()).containsExactly(prefijo+"a");
        assertThat(territorio.parcelas("POINT(-68.8448 -32.8895)",20)).extracting(f->f.id())
            .contains(prefijo+"a").doesNotContain(prefijo+"b",prefijo+"c",prefijo+"d");
    }
    @Test void resumenCuentaElGrupoMixtoComoConflicto(){
        var antes=calidad.resumen();
        jdbc.update("UPDATE territorio_elementos SET duplicado_de=NULL,tipo_duplicado=NULL,campos_conflicto=NULL WHERE capa='catastro' AND origen_id LIKE ?",prefijo+"%");
        var sinGrupo=calidad.resumen();
        assertThat(antes.gruposCoincidentes()-sinGrupo.gruposCoincidentes()).isEqualTo(1);
        assertThat(antes.gruposConConflicto()-sinGrupo.gruposConConflicto()).isEqualTo(1);
        assertThat(antes.copiasIdenticas()-sinGrupo.copiasIdenticas()).isEqualTo(1);
        assertThat(antes.copiasConConflicto()-sinGrupo.copiasConConflicto()).isEqualTo(2);
        assertThat(antes.registrosEnGruposConConflicto()-sinGrupo.registrosEnGruposConConflicto()).isEqualTo(4);
    }
    @Test void clasificacionConviveConGuardadasFichasReportesEHistorialDe48(){
        String email="issue49-"+UUID.randomUUID()+"@ejemplo.invalid";
        var usuario=usuarios.saveAndFlush(new UsuarioEntity("Fixture #49",email,"hash-no-autenticable",
            roles.findByNombre(RolEntity.NombreRol.ADMIN).orElseThrow()));
        var nueva=espacio.guardar(email,prefijo+"c",new Guardar(-32.8895,-68.8448,null));
        assertThat(nueva.parcelaId()).isEqualTo(prefijo+"a");
        var antiguas=new ArrayList<Long>();
        for(String copia:List.of(prefijo+"b",prefijo+"c")){
            var antigua=new ConsultaGuardada(usuario,copia,nomenclatura);
            antigua.actualizar(-32.8895,-68.8448,"Histórica");
            antigua=guardadas.saveAndFlush(antigua);antiguas.add(antigua.getId());
            var informe=actividad.informeGuardada(email,antigua.getId());
            assertThat(informe.parcelaId()).isEqualTo(prefijo+"a");
            assertThat(informe.parcelaIdOriginal()).isEqualTo(copia);
            assertThat(guardadas.findById(antigua.getId()).orElseThrow().getParcelaId()).isEqualTo(copia);
            var reporte=reportes.crear(email,new Crear(copia,ReporteDato.Seccion.AGUA,ReporteDato.Tipo.FALTANTE,"Descripción de prueba suficiente"));
            assertThat(reporte.parcelaId()).isEqualTo(copia);assertThat(reporte.parcelaIdCanonico()).isEqualTo(prefijo+"a");
            assertThat(jdbc.queryForObject("SELECT parcela_id FROM reportes_datos WHERE id=?",String.class,reporte.id())).isEqualTo(copia);
        }
        assertThatThrownBy(()->actividad.comparar(email,antiguas)).isInstanceOfSatisfying(
            ies.belgrano.lotes.exception.OperacionInvalidaException.class,e->{
                assertThat(e.status).isEqualTo(400);assertThat(e.codigo).isEqualTo("COMPARACION_INVALIDA");
            });
        espacio.guardarFicha(email,prefijo+"b",new Ficha("Dirección copia B","Aporte B","Fuente B","Acta B",null));
        assertThat(espacio.publica(prefijo+"a").observaciones()).isEqualTo("Aporte B");
        espacio.guardarFicha(email,prefijo+"c",new Ficha("Dirección copia C","Aporte C","Fuente C","Acta C",null));
        var ficha=espacio.publica(prefijo+"a");
        assertThat(ficha.multiplesAportes()).isTrue();assertThat(ficha.observaciones()).contains("Aporte B","Aporte C");
        assertThat(ficha.aportesRelacionados()).extracting(a->a.parcelaId()).containsExactly(prefijo+"b",prefijo+"c");
        actividad.consultar(email,-32.8895,-68.8448);
        Long id=actividad.historial(email,0).items().getFirst().id();
        String snapshot=historial.findById(id).orElseThrow().getSnapshot();
        aplicar();
        assertThat(actividad.informeHistorico(email,id).historico()).isTrue();
        assertThat(jdbc.queryForObject("SELECT snapshot FROM consultas_historial WHERE id=?",String.class,id)).isEqualTo(snapshot);
    }
}
