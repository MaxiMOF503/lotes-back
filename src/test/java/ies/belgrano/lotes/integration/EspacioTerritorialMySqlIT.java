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
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Todos los datos personales creados por estas pruebas se revierten al terminar. */
@SpringBootTest @Transactional
@EnabledIfEnvironmentVariable(named="TERRITORIO_IT",matches="true")
class EspacioTerritorialMySqlIT {
 @Autowired EspacioTerritorialService service;
 @Autowired TerritorioService territorio;
 @Autowired UsuarioRepository usuarios;
 @Autowired RolRepository roles;
 @Autowired FichaParcelaRepository fichas;
 String ana,bea,id;
 @BeforeEach void fixture(){
   ana="it-"+UUID.randomUUID()+"@ejemplo.invalid";bea="it-"+UUID.randomUUID()+"@ejemplo.invalid";
   var rol=roles.findByNombre(RolEntity.NombreRol.USUARIO).orElseThrow();
   usuarios.saveAndFlush(new UsuarioEntity("Prueba",ana,"hash-no-autenticable",rol));
   usuarios.saveAndFlush(new UsuarioEntity("Prueba",bea,"hash-no-autenticable",rol));
   id=territorio.consultar(-33.0688,-68.8929).parcelas().getFirst().id();
 }
 @Test void consultaSeConservaUnaSolaVezYEsPrivada(){
   var r=new Guardar(-33.0688,-68.8929,null);
   var primera=service.guardar(ana,id,r);var segunda=service.guardar(ana,id,r);
   assertThat(segunda.id()).isEqualTo(primera.id());
   assertThat(service.guardadas(ana,0).total()).isEqualTo(1);
   assertThat(service.guardadas(bea,0).total()).isZero();
   assertThatThrownBy(()->service.quitar(bea,primera.id())).isInstanceOf(OperacionInvalidaException.class);
   assertThat(service.guardadas(ana,0).total()).isEqualTo(1);
   service.quitar(ana,primera.id());
   assertThat(service.guardadas(ana,0).total()).isZero();
 }
 @Test void aporteSePublicaSeparadoYConControlDeVersion(){
   var original=territorio.parcela(id);
   var actual=service.ficha(id);
   var result=service.guardarFicha(ana,id,new Ficha("Dirección de prueba transaccional","Observación",
      "Prueba automatizada","Referencia de test",actual.version()));
   assertThat(result.version()).isNotNull();
   assertThat(result.actualizadoPor()).isEqualTo(ana);
   assertThat(service.publica(id).direccion()).isEqualTo("Dirección de prueba transaccional");
   assertThat(territorio.parcela(id)).isEqualTo(original);
   assertThatThrownBy(()->service.guardarFicha(ana,id,new Ficha("Otro","Obs","Fuente","Ref",actual.version())))
      .isInstanceOf(OperacionInvalidaException.class);
 }
 @Test void nomenclaturaEncuentraParcelaRealYRechazaComodines(){
   var result=territorio.buscarParcelas("060406000300002300007",0);
   assertThat(result.items()).extracting(p->p.properties().path("nomenclatu").asText()).contains("060406000300002300007");
   assertThatThrownBy(()->territorio.buscarParcelas("%",0)).isInstanceOf(OperacionInvalidaException.class);
 }
}
