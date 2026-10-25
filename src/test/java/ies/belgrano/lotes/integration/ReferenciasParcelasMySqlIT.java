package ies.belgrano.lotes.integration;

import ies.belgrano.lotes.dto.request.EspacioTerritorialRequest.*;
import ies.belgrano.lotes.dto.request.ReporteDatoRequest.Crear;
import ies.belgrano.lotes.dto.response.ActividadTerritorialResponse.Informe;
import ies.belgrano.lotes.dto.response.TerritorioResponse.Consulta;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.*;
import ies.belgrano.lotes.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Base de integración con capas importadas. Fixtures personales transaccionales y con rollback. */
@SpringBootTest @Transactional
@EnabledIfEnvironmentVariable(named="TERRITORIO_IT",matches="true")
class ReferenciasParcelasMySqlIT {
    static final String PRINCIPAL="Parcelas_visor0.55136",COPIA="Parcelas_visor0.55137",NOM="060103004400002700000";
    static final double LAT=-33.03834643,LON=-68.88022263;
    @Autowired TerritorioService territorio;
    @Autowired EspacioTerritorialService espacio;
    @Autowired ActividadTerritorialService actividad;
    @Autowired ReporteDatoService reportes;
    @Autowired ConsultaGuardadaRepository guardadas;
    @Autowired ConsultaHistorialRepository historial;
    @Autowired FichaParcelaRepository fichas;
    @Autowired ReporteDatoRepository reportesRepo;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    UsuarioEntity usuario;
    String email;
    @BeforeEach void preparar(){
        email="referencia-"+UUID.randomUUID()+"@ejemplo.invalid";
        usuario=usuarios.saveAndFlush(new UsuarioEntity("Prueba aislada",email,"hash-no-autenticable",
            roles.findByNombre(RolEntity.NombreRol.ADMIN).orElseThrow()));
    }
    ConsultaGuardada antigua(String id){
        var g=new ConsultaGuardada(usuario,id,NOM);g.actualizar(LAT,LON,"Histórica");
        return guardadas.saveAndFlush(g);
    }
    @Test void casoRealResuelveCopiaYNoReintroducePoligonosRepetidos(){
        assertThat(territorio.resolverParcela(PRINCIPAL).idCanonico()).isEqualTo(PRINCIPAL);
        var r=territorio.resolverParcela(COPIA);
        assertThat(r.idOriginal()).isEqualTo(COPIA);assertThat(r.idCanonico()).isEqualTo(PRINCIPAL);
        assertThat(r.idsRelacionados()).contains(PRINCIPAL,COPIA);
        assertThat(territorio.parcela(COPIA).id()).isEqualTo(COPIA);
        assertThat(territorio.buscarParcelas(NOM,0).items()).extracting(p->p.id()).containsExactly(PRINCIPAL);
        assertThat(territorio.consultar(LAT,LON).parcelas()).extracting(p->p.id()).containsExactly(PRINCIPAL);
        assertThatThrownBy(()->territorio.resolverParcela("referencia-inexistente"))
            .isInstanceOfSatisfying(OperacionInvalidaException.class,e->assertThat(e.status).isEqualTo(404));
    }
    @Test void informeYComparacionAdmitenGuardadaHistoricaDeCopia(){
        var copia=antigua(COPIA);
        var r=actividad.informeGuardada(email,copia.getId());
        assertThat(r.parcelaId()).isEqualTo(PRINCIPAL);assertThat(r.parcelaIdOriginal()).isEqualTo(COPIA);
        assertThat(guardadas.findById(copia.getId()).orElseThrow().getParcelaId()).isEqualTo(COPIA);
        var otraId=territorio.consultar(-33.0688,-68.8929).parcelas().getFirst().id();
        var otra=espacio.guardar(email,otraId,new Guardar(-33.0688,-68.8929,null));
        assertThat(actividad.comparar(email,List.of(copia.getId(),otra.id()))).extracting(Informe::parcelaId).containsExactly(PRINCIPAL,otraId);
        var principal=antigua(PRINCIPAL);
        assertThatThrownBy(()->actividad.comparar(email,List.of(copia.getId(),principal.getId())))
            .isInstanceOfSatisfying(OperacionInvalidaException.class,e->assertThat(e.codigo).isEqualTo("COMPARACION_INVALIDA"));
    }
    @Test void nuevasGuardadasSeUnifican(){
        var primera=espacio.guardar(email,COPIA,new Guardar(LAT,LON,null));
        var repetida=espacio.guardar(email,PRINCIPAL,new Guardar(LAT,LON,"Repetida"));
        assertThat(primera.parcelaId()).isEqualTo(PRINCIPAL);assertThat(repetida.id()).isEqualTo(primera.id());
        assertThat(espacio.guardadas(email,0).total()).isEqualTo(1);
    }
    @Test void guardarRepresentanteReutilizaLaCopiaYaAlmacenada(){
        var anterior=antigua(COPIA);
        var respuesta=espacio.guardar(email,PRINCIPAL,new Guardar(LAT,LON,null));
        assertThat(respuesta.id()).isEqualTo(anterior.getId());assertThat(respuesta.parcelaId()).isEqualTo(COPIA);
        assertThat(respuesta.etiqueta()).isEqualTo("Histórica");
        assertThat(espacio.guardadas(email,0).total()).isEqualTo(1);
    }
    @Test void dosGuardadasHistoricasEquivalentesNoSeBorranYSeReutilizaElMenorId(){
        var masAntigua=antigua(COPIA);var posterior=antigua(PRINCIPAL);
        var fecha=masAntigua.getCreadaEn();
        var respuesta=espacio.guardar(email,PRINCIPAL,new Guardar(LAT,LON,null));
        assertThat(respuesta.id()).isEqualTo(masAntigua.getId());assertThat(respuesta.parcelaId()).isEqualTo(COPIA);
        assertThat(respuesta.creadaEn()).isEqualTo(fecha);assertThat(respuesta.etiqueta()).isEqualTo("Histórica");
        assertThat(guardadas.findById(posterior.getId())).isPresent();
        assertThat(espacio.guardadas(email,0).total()).isEqualTo(2);
    }
    @Test void fichasDeAmbosIdsSeConservanYSonLocalizablesSinElegirUna(){
        Assumptions.assumeTrue(!fichas.existsById(PRINCIPAL)&&!fichas.existsById(COPIA),"Requiere fixtures sin fichas previas");
        espacio.guardarFicha(email,COPIA,new Ficha("Dirección copia","Aporte copia","Fuente copia","Acta copia",null));
        assertThat(espacio.publica(PRINCIPAL).observaciones()).isEqualTo("Aporte copia");
        espacio.guardarFicha(email,PRINCIPAL,new Ficha("Dirección representante","Aporte representante","Fuente representante","Acta representante",null));
        var publica=espacio.publica(PRINCIPAL);
        assertThat(publica.multiplesAportes()).isTrue();assertThat(publica.aportesRelacionados()).hasSize(2);
        assertThat(publica.observaciones()).contains(PRINCIPAL,COPIA,"Aporte copia","Aporte representante","Acta copia");
        assertThat(publica.actualizadoEn()).isNotNull();
        assertThat(publica.aportesRelacionados()).extracting(a->a.parcelaId()).containsExactly(PRINCIPAL,COPIA);
        assertThat(espacio.ficha(PRINCIPAL).aportesRelacionados()).extracting(f->f.parcelaId()).containsExactly(COPIA);
        assertThat(fichas.findById(COPIA).orElseThrow().getObservaciones()).isEqualTo("Aporte copia");
    }
    @Test void reportesPreservanAmbosIdsYDevuelvenElMismoCanonico(){
        for(String id:List.of(PRINCIPAL,COPIA)){
            var r=reportes.crear(email,new Crear(id,ReporteDato.Seccion.AGUA,ReporteDato.Tipo.FALTANTE,"Falta información de prueba"));
            assertThat(r.parcelaId()).isEqualTo(id);assertThat(r.parcelaIdCanonico()).isEqualTo(PRINCIPAL);
            assertThat(reportesRepo.findById(r.id()).orElseThrow().getParcelaId()).isEqualTo(id);
        }
    }
    @Test void snapshotConCopiaSeLeeSinModificarElJsonPersistido(){
        var actual=territorio.consultar(LAT,LON);
        var copia=new Consulta(LAT,LON,List.of(territorio.parcela(COPIA)),false,actual.dentroRadioAgua(),
            actual.dentroRadioAguasLujan(),actual.electricidad(),actual.fuentes(),actual.advertencia());
        var mapper=JsonMapper.builder().build();var fecha=Instant.now();
        String json=mapper.writeValueAsString(new Informe(fecha,fecha,false,COPIA,copia,List.of()));
        var h=historial.saveAndFlush(new ConsultaHistorial(usuario,LAT,LON,fecha,NOM,json));
        assertThat(actividad.informeHistorico(email,h.getId()).consulta().parcelas().getFirst().id()).isEqualTo(COPIA);
        assertThat(historial.findById(h.getId()).orElseThrow().getSnapshot()).isEqualTo(json);
    }
}
