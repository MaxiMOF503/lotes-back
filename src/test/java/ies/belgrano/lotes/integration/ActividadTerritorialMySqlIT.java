package ies.belgrano.lotes.integration;
import ies.belgrano.lotes.service.*;
import ies.belgrano.lotes.repository.*;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.dto.request.EspacioTerritorialRequest.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
@EnabledIfEnvironmentVariable(named="TERRITORIO_IT",matches="true")
class ActividadTerritorialMySqlIT {
 @Autowired ActividadTerritorialService service;
 @Autowired EspacioTerritorialService espacio;
 @Autowired TerritorioService territorio;
 @Autowired UsuarioRepository usuarios;
 @Autowired RolRepository roles;
 String ana,bea,id;
 @BeforeEach void preparar(){
   ana="it-"+UUID.randomUUID()+"@ejemplo.invalid";bea="it-"+UUID.randomUUID()+"@ejemplo.invalid";
   var rol=roles.findByNombre(RolEntity.NombreRol.USUARIO).orElseThrow();
   usuarios.saveAndFlush(new UsuarioEntity("Prueba",ana,"hash-no-autenticable",rol));
   usuarios.saveAndFlush(new UsuarioEntity("Prueba",bea,"hash-no-autenticable",rol));
   id=territorio.consultar(-33.0688,-68.8929).parcelas().getFirst().id();
 }
 @Test void snapshotMantieneElAporteOriginalYLaFecha(){
   var ficha=espacio.ficha(id);
   espacio.guardarFicha(ana,id,new Ficha(null,"Aporte original","Fuente transaccional","Referencia original",ficha.version()));
   var consulta=service.consultar(ana,-33.0688,-68.8929);
   var h=service.historial(ana,0).items().getFirst();
   var anterior=espacio.ficha(id);
   espacio.guardarFicha(ana,id,new Ficha(null,"Aporte posterior","Fuente transaccional","Referencia posterior",anterior.version()));
   var r=service.informeHistorico(ana,h.id());
   assertThat(r.historico()).isTrue();assertThat(r.fechaConsulta()).isEqualTo(h.consultadaEn());
   assertThat(r.consulta()).isEqualTo(consulta);
   assertThat(r.aportes()).anySatisfy(a->{assertThat(a.parcelaId()).isEqualTo(id);assertThat(a.ficha().observaciones()).isEqualTo("Aporte original");});
 }
 @Test void historialPrivadoSeQuitaSinEliminarGuardadas(){
   var g=espacio.guardar(ana,id,new Guardar(-33.0688,-68.8929,null));
   service.consultar(ana,-33.0688,-68.8929);var h=service.historial(ana,0).items().getFirst();
   assertThat(service.historial(bea,0).total()).isZero();
   assertThatThrownBy(()->service.informeHistorico(bea,h.id())).isInstanceOf(OperacionInvalidaException.class);
   assertThatThrownBy(()->service.quitarHistorial(bea,h.id())).isInstanceOf(OperacionInvalidaException.class);
   service.quitarHistorial(ana,h.id());assertThat(service.historial(ana,0).total()).isZero();
   assertThat(espacio.guardadas(ana,0).items()).extracting(f->f.id()).contains(g.id());
 }
 @Test void informesYComparacionUsanParcelaSeleccionadaYPermisos(){
   var g=espacio.guardar(ana,id,new Guardar(-33.0688,-68.8929,null));
   var r=service.informeGuardada(ana,g.id());assertThat(r.parcelaId()).isEqualTo(id);assertThat(r.historico()).isFalse();
   assertThatThrownBy(()->service.informeGuardada(bea,g.id())).isInstanceOf(OperacionInvalidaException.class);
   var otra=territorio.consultar(-33.04321,-68.88668).parcelas().stream().filter(p->!p.id().equals(id)).findFirst().orElseThrow();
   var segundo=espacio.guardar(ana,otra.id(),new Guardar(-33.04321,-68.88668,null));
   var comparacion=service.comparar(ana,List.of(g.id(),segundo.id()));
   assertThat(comparacion).hasSize(2);assertThat(comparacion).extracting(i->i.parcelaId()).containsExactly(id,otra.id());
   assertThat(comparacion.getFirst().fechaConsulta()).isEqualTo(comparacion.getLast().fechaConsulta());
   assertThat(service.historial(ana,0).total()).isZero();
   assertThatThrownBy(()->service.comparar(bea,List.of(g.id(),segundo.id()))).isInstanceOf(OperacionInvalidaException.class);
 }
}
