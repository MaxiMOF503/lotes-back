package ies.belgrano.lotes.integration;

import ies.belgrano.lotes.service.EstadoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** Solo lectura sobre la base con las capas ya importadas. No depende de que Georef responda. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="TERRITORIO_IT",matches="true")
class EstadoMySqlIT {
    @Autowired EstadoService service;

    @Test void conLasCapasImportadasElEstadoLasReconoceYInformaLaVersionDelEsquema() {
        var estado=service.estado();
        assertThat(estado.baseDatos()).isEqualTo("OK");
        assertThat(estado.esquema()).isNotBlank();
        assertThat(estado.capasFaltantes()).isEmpty();
        assertThat(estado.capas()).filteredOn(c -> c.requerida()).extracting(c -> c.capa()).containsExactly("catastro","aysam","electricidad");
        assertThat(estado.capas()).filteredOn(c -> c.requerida()).allSatisfy(c -> {
            assertThat(c.importada()).isTrue();
            assertThat(c.registros()).isPositive();
        });
        assertThat(estado.estado()).isIn("OK","DEGRADADO");   // DEGRADADO solo si Georef no responde desde donde se corre
    }
}
