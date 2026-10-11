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
    @Test void ventanaRespetaLimiteYConservaOrdenGeojsonLongitudLatitud() {
        var result=service.capa("catastro",-68.9,-33.074,-68.888,-33.065,16,10);
        assertThat(result.features()).hasSize(10);
        assertThat(result.hayMas()).isTrue();
        var coordinates=result.features().getFirst().geometry().get("coordinates").get(0).get(0).get(0);
        assertThat(coordinates.get(0).asDouble()).isBetween(-69.0,-68.8);
        assertThat(coordinates.get(1).asDouble()).isBetween(-33.1,-33.0);
    }
}
