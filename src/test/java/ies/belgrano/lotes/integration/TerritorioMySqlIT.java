package ies.belgrano.lotes.integration;

import ies.belgrano.lotes.service.TerritorioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

/** Solo lectura sobre los tres archivos ya importados. No crea ni borra fixtures. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="TERRITORIO_IT", matches="true")
class TerritorioMySqlIT {
    @Autowired TerritorioService service;

    @Test void puntoRealIdentificaParcelaYCalculaDistanciaEnMetrosSinInvertirEjes() {
        var result=service.consultar(-33.0688,-68.8929);
        assertThat(result.parcelas()).hasSize(1);
        assertThat(result.parcelas().getFirst().properties().get("nomenclatu").asText())
            .isEqualTo("060406000300002300007");
        assertThat(result.electricidad().distanciaMetros()).isBetween(25.0,32.0);
        assertThat(result.electricidad().tipo()).isEqualTo("13.2 kV");
        assertThat(result.dentroRadioAgua()).isFalse();
    }
    @Test void puntoEnVerticeDelRadioDeAguaSeIncluyeEnLaCoberturaGeneral() {
        assertThat(service.consultar(-33.04570011,-68.92206571).dentroRadioAgua()).isTrue();
    }
    @Test void fueraDelDatasetNoInventaParcelaNiRedElectrica() {
        var result=service.consultar(-34,-70);
        assertThat(result.parcelas()).isEmpty();
        assertThat(result.dentroRadioAgua()).isFalse();
        assertThat(result.electricidad().distanciaMetros()).isNull();
    }
    @Test void parcelaRepetidaEnLaFuenteSeDevuelveUnaSolaVez() {
        // La fuente municipal publica 060103004400002700000 ocho veces con la misma geometría.
        var consulta=service.consultar(-33.03834643,-68.88022263);
        assertThat(consulta.parcelas()).hasSize(1);
        assertThat(consulta.hayMasParcelas()).isFalse();
        assertThat(consulta.parcelas().getFirst().id()).isEqualTo("Parcelas_visor0.55136");
        var busqueda=service.buscarParcelas("060103004400002700000",0);
        assertThat(busqueda.items()).extracting(f->f.id()).containsExactly("Parcelas_visor0.55136");
    }
    @Test void copiaRepetidaSigueDisponiblePorSuIdentificador() {
        // Fichas, consultas guardadas o historial pueden referirse a una copia.
        assertThat(service.parcela("Parcelas_visor0.55137").properties().get("nomenclatu").asText())
            .isEqualTo("060103004400002700000");
    }
    @Test void parcelasCercanasAlPuntoDeGeorefVanDeLaMasCercanaALaMasLejanaSinCopias() {
        // Punto que Georef devuelve para "San Martín 1500": cae sobre la calle, no dentro de una parcela.
        assertThat(service.consultar(-33.0262588845649,-68.8749132362673).parcelas()).isEmpty();
        var cercanas=service.cercanas(-33.0262588845649,-68.8749132362673,30).parcelas();
        assertThat(cercanas).isNotEmpty().allSatisfy(c -> assertThat(c.distanciaMetros()).isBetween(0.0,30.0));
        assertThat(cercanas).extracting(c -> c.distanciaMetros()).isSorted();
        assertThat(cercanas.getFirst().distanciaMetros()).isBetween(8.0,15.0);
        // Esquina San Martín y Sáenz Peña: la parcela repetida en la fuente aparece una sola vez.
        var esquina=service.cercanas(-33.0385356569091,-68.8800290274383,30).parcelas();
        assertThat(esquina).filteredOn(c -> c.parcela().properties().get("nomenclatu").asText().equals("060103004400002700000"))
            .singleElement().satisfies(c -> assertThat(c.distanciaMetros()).isBetween(12.0,18.0));
    }
    @Test void ventanaRespetaLimiteYConservaOrdenGeojsonLongitudLatitud() {
        var result=service.capa("catastro",-68.9,-33.074,-68.888,-33.065,16,10);
        assertThat(result.features()).hasSize(10);
        assertThat(result.hayMas()).isTrue();
        var coordinates=result.features().getFirst().geometry().get("coordinates").get(0).get(0).get(0);
        assertThat(coordinates.get(0).asDouble()).isBetween(-69.0,-68.8);
        assertThat(coordinates.get(1).asDouble()).isBetween(-33.1,-33.0);
    }
}
