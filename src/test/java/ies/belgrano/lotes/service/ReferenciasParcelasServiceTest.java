package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.request.EspacioTerritorialRequest.Guardar;
import ies.belgrano.lotes.dto.request.ReporteDatoRequest.Crear;
import ies.belgrano.lotes.dto.response.ActividadTerritorialResponse.*;
import ies.belgrano.lotes.dto.response.TerritorioResponse.*;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Servicios reales con repositorios simulados: no necesita MySQL ni cambia datos locales. */
class ReferenciasParcelasServiceTest {
    static final String PRINCIPAL="Parcelas_visor0.55136", COPIA="Parcelas_visor0.55137", OTRA="parcela.otra";
    static final String NOMENCLATURA="060103004400002700000", EMAIL="ana@ejemplo.invalid";
    final JsonMapper mapper=JsonMapper.builder().build();
    final TerritorioRepository geo=mock(TerritorioRepository.class);
    final ConsultaGuardadaRepository guardadas=mock(ConsultaGuardadaRepository.class);
    final ConsultaHistorialRepository historial=mock(ConsultaHistorialRepository.class);
    final FichaParcelaRepository fichas=mock(FichaParcelaRepository.class);
    final UsuarioRepository usuarios=mock(UsuarioRepository.class);
    final ReporteDatoRepository reportes=mock(ReporteDatoRepository.class);
    final TerritorioService territorio=new TerritorioService(geo);
    final EspacioTerritorialService espacio=new EspacioTerritorialService(guardadas,fichas,usuarios,territorio,geo);
    final ActividadTerritorialService actividad=new ActividadTerritorialService(historial,guardadas,usuarios,territorio,geo,espacio,mapper);
    final ReporteDatoService reportador=new ReporteDatoService(reportes,usuarios,territorio);
    final UsuarioEntity usuario=new UsuarioEntity("Ana",EMAIL,"no-autenticable",new RolEntity(RolEntity.NombreRol.USUARIO));

    @BeforeEach void preparar(){
        when(geo.fuentes()).thenReturn(List.of(fuente("catastro"),fuente("aysam"),fuente("electricidad")));
        for(String id:List.of(PRINCIPAL,COPIA,OTRA)){
            when(geo.parcela(id)).thenReturn(Optional.of(feature(id)));
            when(geo.enlaceParcela(id)).thenReturn(Optional.of(new TerritorioRepository.EnlaceParcela(id,id.equals(COPIA)?PRINCIPAL:null)));
        }
        when(geo.referenciasDelRepresentante(PRINCIPAL)).thenReturn(List.of(PRINCIPAL,COPIA));
        when(geo.referenciasDelRepresentante(OTRA)).thenReturn(List.of(OTRA));
        when(geo.pertenece(anyString(),anyString())).thenReturn(true);
        when(geo.parcelas(anyString(),eq(6))).thenReturn(List.of(feature(PRINCIPAL)));
        when(geo.electricidad(anyString(),anyString(),eq(1000))).thenReturn(new Electricidad(null,null,1000,"Prueba"));
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(guardadas.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        when(reportes.save(any())).thenAnswer(i->i.getArgument(0));
    }
    Fuente fuente(String capa){return new Fuente(capa,"Fuente de prueba","https://example.invalid",1,0,0,"2026-10-25");}
    Feature feature(String id){return new Feature("Feature",id,mapper.readTree("{}"),mapper.readTree("{\"nomenclatu\":\""+NOMENCLATURA+"\"}"));}
    ConsultaGuardada guardada(String id){var g=new ConsultaGuardada(usuario,id,NOMENCLATURA);g.actualizar(-33,-68.8,"Histórica");return g;}
    FichaParcela ficha(String id,String texto){var f=new FichaParcela(id);f.actualizar("Dirección "+id,texto,"Fuente "+id,"Acta "+id,EMAIL);return f;}

    @Test void representanteYCopiaConservanIdentidadOriginalYResuelvenElMismoCanonico(){
        var p=territorio.resolverParcela(PRINCIPAL);var c=territorio.resolverParcela(COPIA);
        assertThat(p.idCanonico()).isEqualTo(PRINCIPAL);assertThat(p.esCopia()).isFalse();
        assertThat(c.idOriginal()).isEqualTo(COPIA);assertThat(c.idCanonico()).isEqualTo(PRINCIPAL);assertThat(c.esCopia()).isTrue();
        assertThat(c.idsRelacionados()).containsExactly(PRINCIPAL,COPIA).doesNotContain(OTRA);
        assertThat(territorio.parcela(COPIA).id()).isEqualTo(COPIA);
    }
    @Test void inexistenteConserva404YSinReferenciasNoInventaUnaParcela(){
        assertThatThrownBy(()->territorio.resolverParcela("inexistente"))
            .isInstanceOfSatisfying(OperacionInvalidaException.class,e->{assertThat(e.status).isEqualTo(404);assertThat(e.codigo).isEqualTo("PARCELA_NO_ENCONTRADA");});
    }
    @Test void ciclosYRepresentantesAusentesDanConflictoControlado(){
        when(geo.enlaceParcela(PRINCIPAL)).thenReturn(Optional.of(new TerritorioRepository.EnlaceParcela(PRINCIPAL,COPIA)));
        assertThatThrownBy(()->territorio.resolverParcela(COPIA)).isInstanceOfSatisfying(OperacionInvalidaException.class,e->assertThat(e.codigo).isEqualTo("REFERENCIA_PARCELA_INVALIDA"));
        when(geo.enlaceParcela(PRINCIPAL)).thenReturn(Optional.of(new TerritorioRepository.EnlaceParcela(PRINCIPAL,"ausente")));
        assertThatThrownBy(()->territorio.resolverParcela(COPIA)).isInstanceOf(OperacionInvalidaException.class);
    }
    @Test void resuelveCadenasDeCopias(){
        when(geo.enlaceParcela(COPIA)).thenReturn(Optional.of(new TerritorioRepository.EnlaceParcela(COPIA,OTRA)));
        when(geo.enlaceParcela(OTRA)).thenReturn(Optional.of(new TerritorioRepository.EnlaceParcela(OTRA,PRINCIPAL)));
        assertThat(territorio.resolverParcela(COPIA).idCanonico()).isEqualTo(PRINCIPAL);
    }
    @Test void unaAutorreferenciaNoSeConfundeConUnRepresentante(){
        when(geo.enlaceParcela(PRINCIPAL)).thenReturn(Optional.of(new TerritorioRepository.EnlaceParcela(PRINCIPAL,PRINCIPAL)));
        assertThatThrownBy(()->territorio.resolverParcela(PRINCIPAL)).isInstanceOfSatisfying(OperacionInvalidaException.class,e->assertThat(e.codigo).isEqualTo("REFERENCIA_PARCELA_INVALIDA"));
        verify(geo,never()).referenciasDelRepresentante(anyString());
    }
    @Test void informeDeCopiaUsaRepresentanteSinCambiarLaGuardada(){
        var g=guardada(COPIA);when(guardadas.findByIdAndUsuarioEmail(1L,EMAIL)).thenReturn(Optional.of(g));
        var r=actividad.informeGuardada(EMAIL,1L);
        assertThat(r.parcelaId()).isEqualTo(PRINCIPAL);assertThat(r.parcelaIdOriginal()).isEqualTo(COPIA);
        assertThat(r.consulta().parcelas()).extracting(Feature::id).containsExactly(PRINCIPAL);
        assertThat(g.getParcelaId()).isEqualTo(COPIA);verify(guardadas,never()).save(any());
    }
    @Test void informeDelRepresentanteContinuaFuncionando(){
        when(guardadas.findByIdAndUsuarioEmail(1L,EMAIL)).thenReturn(Optional.of(guardada(PRINCIPAL)));
        assertThat(actividad.informeGuardada(EMAIL,1L).parcelaIdOriginal()).isEqualTo(PRINCIPAL);
    }
    @Test void permiteCompararCopiaConOtraParcelaYRechazaEquivalentes(){
        when(guardadas.findByIdAndUsuarioEmail(1L,EMAIL)).thenReturn(Optional.of(guardada(COPIA)));
        when(guardadas.findByIdAndUsuarioEmail(2L,EMAIL)).thenReturn(Optional.of(guardada(OTRA)));
        when(geo.parcelas(anyString(),eq(6))).thenReturn(List.of(feature(PRINCIPAL),feature(OTRA)));
        assertThat(actividad.comparar(EMAIL,List.of(1L,2L))).extracting(Informe::parcelaId).containsExactly(PRINCIPAL,OTRA);
        when(guardadas.findByIdAndUsuarioEmail(2L,EMAIL)).thenReturn(Optional.of(guardada(PRINCIPAL)));
        assertThatThrownBy(()->actividad.comparar(EMAIL,List.of(1L,2L))).isInstanceOfSatisfying(OperacionInvalidaException.class,e->{assertThat(e.status).isEqualTo(400);assertThat(e.codigo).isEqualTo("COMPARACION_INVALIDA");});
    }
    @Test void nuevaGuardadaUsaCanonicoYLaAntiguaCopiaSeReutiliza(){
        var nueva=espacio.guardar(EMAIL,COPIA,new Guardar(-33.0,-68.8,null));
        assertThat(nueva.parcelaId()).isEqualTo(PRINCIPAL);
        var antigua=guardada(COPIA);
        when(guardadas.findFirstByUsuarioEmailAndParcelaIdInOrderByIdAsc(EMAIL,List.of(PRINCIPAL,COPIA))).thenReturn(Optional.of(antigua));
        var fecha=antigua.getCreadaEn();
        assertThat(espacio.guardar(EMAIL,PRINCIPAL,new Guardar(-33.0,-68.8,"Actualizada")).parcelaId()).isEqualTo(COPIA);
        assertThat(antigua.getCreadaEn()).isEqualTo(fecha);
    }
    @Test void fichaDeCopiaEsVisibleDesdeRepresentanteSinExponerAutor(){
        when(fichas.findAllById(List.of(PRINCIPAL,COPIA))).thenReturn(List.of(ficha(COPIA,"Aporte original")));
        var publica=espacio.publica(PRINCIPAL);
        assertThat(publica.observaciones()).isEqualTo("Aporte original");
        assertThat(publica.direccion()).isEqualTo("Dirección "+COPIA);
        assertThat(publica.fuente()).isEqualTo("Fuente "+COPIA);assertThat(publica.multiplesAportes()).isFalse();
        assertThat(publica.aportesRelacionados().getFirst().parcelaId()).isEqualTo(COPIA);
        assertThat(mapper.writeValueAsString(publica)).doesNotContain(EMAIL,"actualizadoPor","version");
    }
    @Test void ambasFichasSeExponenSeparadasYLaEdicionSigueSiendoPorIdExacto(){
        var a=ficha(PRINCIPAL,"Aporte A");var b=ficha(COPIA,"Aporte B");
        when(fichas.findAllById(List.of(PRINCIPAL,COPIA))).thenReturn(List.of(b,a));
        when(fichas.findById(PRINCIPAL)).thenReturn(Optional.of(a));
        var publica=espacio.publica(COPIA);
        assertThat(publica.multiplesAportes()).isTrue();assertThat(publica.direccion()).isNull();
        assertThat(publica.aportesRelacionados()).extracting(f->f.observaciones()).containsExactly("Aporte A","Aporte B");
        assertThat(publica.observaciones()).contains(PRINCIPAL,COPIA,"Aporte A","Aporte B","Dirección "+COPIA,"Acta "+PRINCIPAL);
        assertThat(publica.fuente()).isNotBlank();assertThat(publica.referencia()).isNotBlank();
        assertThat(publica.actualizadoEn()).isEqualTo(b.getActualizadoEn());
        var admin=espacio.ficha(PRINCIPAL);
        assertThat(admin.observaciones()).isEqualTo("Aporte A");assertThat(admin.aportesRelacionados()).extracting(f->f.parcelaId()).containsExactly(COPIA);
        assertThat(admin.aportesRelacionados().getFirst().idCanonico()).isEqualTo(PRINCIPAL);
        verify(fichas,never()).delete(any());
    }
    @Test void soloRepresentanteNoPresentaConflictoYConservaCamposLegacy(){
        var f=ficha(PRINCIPAL,"Único aporte");
        when(fichas.findAllById(List.of(PRINCIPAL,COPIA))).thenReturn(List.of(f));
        var publica=espacio.publica(COPIA);
        assertThat(publica.multiplesAportes()).isFalse();
        assertThat(publica.direccion()).isEqualTo(f.getDireccion());assertThat(publica.fuente()).isEqualTo(f.getFuente());
        assertThat(publica.aportesRelacionados()).extracting(a->a.parcelaId()).containsExactly(PRINCIPAL);
    }
    @Test void variasCopiasSinFichaRepresentanteSonVisiblesTambienEnElContratoAnterior(){
        String segunda="Parcelas_visor0.55138";
        when(geo.referenciasDelRepresentante(PRINCIPAL)).thenReturn(List.of(segunda,COPIA,PRINCIPAL,COPIA));
        when(fichas.findAllById(List.of(PRINCIPAL,COPIA,segunda))).thenReturn(List.of(ficha(segunda,"Segundo aporte"),ficha(COPIA,"Primer aporte")));
        var publica=espacio.publica(PRINCIPAL);
        assertThat(publica.multiplesAportes()).isTrue();assertThat(publica.direccion()).isNull();
        assertThat(publica.aportesRelacionados()).extracting(a->a.parcelaId()).containsExactly(COPIA,segunda);
        assertThat(publica.observaciones()).contains(COPIA,segunda,"Primer aporte","Segundo aporte","Fuente "+segunda,"Acta "+COPIA);
        assertThat(publica.actualizadoEn()).isNotNull();
    }
    @Test void reutilizarEnAmbasDireccionesNoBorraEtiquetaNiIdentidadNiFechaSiSeOmite(){
        for(String existente:List.of(PRINCIPAL,COPIA)){
            var antigua=guardada(existente);var fecha=antigua.getCreadaEn();
            when(guardadas.findFirstByUsuarioEmailAndParcelaIdInOrderByIdAsc(EMAIL,List.of(PRINCIPAL,COPIA))).thenReturn(Optional.of(antigua));
            var respuesta=espacio.guardar(EMAIL,existente.equals(COPIA)?PRINCIPAL:COPIA,new Guardar(-33.0,-68.8,null));
            assertThat(respuesta.parcelaId()).isEqualTo(existente);assertThat(respuesta.etiqueta()).isEqualTo("Histórica");
            assertThat(respuesta.creadaEn()).isEqualTo(fecha);
        }
    }
    @Test void metadatoDerivadoNoCargaGeometriasNiConsultaTodosLosHermanos(){
        clearInvocations(geo);
        assertThat(territorio.canonicoDisponible(COPIA)).contains(PRINCIPAL);
        verify(geo,never()).parcela(anyString());verify(geo,never()).referenciasDelRepresentante(anyString());
    }
    @Test void metadatoDerivadoToleraAusenciaDeTablaTerritorialPeroNoOcultaErroresSqlDistintos(){
        when(geo.enlaceParcela(COPIA)).thenThrow(new org.springframework.jdbc.BadSqlGrammarException("prueba","consulta",
            new java.sql.SQLException("tabla ausente","42S02",1146)));
        assertThat(territorio.canonicoDisponible(COPIA)).isEmpty();
        doThrow(new org.springframework.jdbc.BadSqlGrammarException("prueba","consulta",
            new java.sql.SQLException("columna incorrecta","42S22",1054))).when(geo).enlaceParcela(COPIA);
        assertThatThrownBy(()->territorio.canonicoDisponible(COPIA)).isInstanceOf(org.springframework.jdbc.BadSqlGrammarException.class);
    }
    @Test void reporteConservaIdOriginalYExponeCanonicoTantoParaCopiaComoRepresentante(){
        for(String id:List.of(COPIA,PRINCIPAL)){
            var r=reportador.crear(EMAIL,new Crear(id,ReporteDato.Seccion.AGUA,ReporteDato.Tipo.FALTANTE,"Falta información publicada"));
            assertThat(r.parcelaId()).isEqualTo(id);assertThat(r.parcelaIdCanonico()).isEqualTo(PRINCIPAL);
        }
    }
    @Test void reporteHistoricoSigueLegibleSiLaParcelaYaNoExiste(){
        var r=new ReporteDato(usuario,"ausente",NOMENCLATURA,ReporteDato.Seccion.AGUA,ReporteDato.Tipo.FALTANTE,"Descripción de prueba",Instant.now());
        when(reportes.findByCreadoPorEmail(eq(EMAIL),any())).thenReturn(new PageImpl<>(List.of(r)));
        assertThat(reportador.propios(EMAIL,0).items().getFirst().parcelaIdCanonico()).isNull();
    }
    @Test void listarReportesNoDependeDeQueLaCapaSigaImportada(){
        var r=new ReporteDato(usuario,COPIA,NOMENCLATURA,ReporteDato.Seccion.AGUA,ReporteDato.Tipo.FALTANTE,"Descripción de prueba",Instant.now());
        when(reportes.findByCreadoPorEmail(eq(EMAIL),any())).thenReturn(new PageImpl<>(List.of(r)));
        when(geo.fuentes()).thenReturn(List.of());
        var respuesta=reportador.propios(EMAIL,0).items().getFirst();
        assertThat(respuesta.parcelaId()).isEqualTo(COPIA);assertThat(respuesta.parcelaIdCanonico()).isNull();
    }
    @Test void cambiarLaResolucionNoReescribeLaFilaDelReporte(){
        var r=new ReporteDato(usuario,COPIA,NOMENCLATURA,ReporteDato.Seccion.AGUA,ReporteDato.Tipo.FALTANTE,"Descripción de prueba",Instant.now());
        when(reportes.findByCreadoPorEmail(eq(EMAIL),any())).thenReturn(new PageImpl<>(List.of(r)));
        assertThat(reportador.propios(EMAIL,0).items().getFirst().parcelaIdCanonico()).isEqualTo(PRINCIPAL);
        when(geo.enlaceParcela(COPIA)).thenReturn(Optional.of(new TerritorioRepository.EnlaceParcela(COPIA,OTRA)));
        assertThat(reportador.propios(EMAIL,0).items().getFirst().parcelaIdCanonico()).isEqualTo(OTRA);
        assertThat(r.getParcelaId()).isEqualTo(COPIA);verify(reportes,never()).save(any());
        assertThat(territorio.canonicoDisponible("id antiguo no válido")).isEmpty();
    }
    @Test void snapshotAntiguoSinCamposNuevosNoSeRecalculaNiReescribe(){
        String json="""
            {"generadoEn":"2026-10-01T12:00:00Z","fechaConsulta":"2026-10-01T12:00:00Z","historico":false,
             "parcelaId":"Parcelas_visor0.55137","consulta":{"latitud":-33,"longitud":-68.8,
             "parcelas":[{"type":"Feature","id":"Parcelas_visor0.55137","geometry":{},"properties":{}}],
             "hayMasParcelas":false,"dentroRadioAgua":false,"electricidad":null,"fuentes":[],"advertencia":"Histórica"},
             "aportes":[{"parcelaId":"Parcelas_visor0.55137","ficha":{"direccion":null,"observaciones":"Original",
             "fuente":"Fuente","referencia":"Acta","actualizadoEn":"2026-10-01T12:00:00Z"}}]}
            """;
        var h=new ConsultaHistorial(usuario,-33,-68.8,Instant.now(),"Histórica",json);
        when(historial.findByIdAndUsuarioEmail(1L,EMAIL)).thenReturn(Optional.of(h));
        clearInvocations(geo);
        var r=actividad.informeHistorico(EMAIL,1L);
        assertThat(r.consulta().parcelas().getFirst().id()).isEqualTo(COPIA);assertThat(r.parcelaId()).isEqualTo(COPIA);
        assertThat(r.aportes().getFirst().ficha().observaciones()).isEqualTo("Original");
        assertThat(h.getSnapshot()).isEqualTo(json);verifyNoInteractions(geo);verify(historial,never()).save(any());
    }
}
