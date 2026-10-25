package ies.belgrano.lotes.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

/** El guard de integración debe rechazar destinos distintos antes de crear beans de conexión. */
class ClasificacionDuplicadosMySqlGuardTest {
    void comprobar(String url){
        try(var contexto=new GenericApplicationContext()){
            contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("prueba",
                Map.of("spring.datasource.url",url)));
            new ClasificacionDuplicadosMySqlIT.BaseAislada().initialize(contexto);
        }
    }
    @Test void rechazaBaseRealODeOtroIssueYServidorRemoto(){
        for(String url:new String[]{"jdbc:mysql://localhost:3306/verificador_lotes",
                "jdbc:mysql://localhost:3306/lotes_issue48_test","jdbc:mysql://servidor.invalid/lotes_issue49_test",
                "jdbc:mysql://localhost/lotes_issue49_test_otro"})
            assertThatThrownBy(()->comprobar(url)).isInstanceOf(IllegalStateException.class);
    }
    @Test void aceptaSoloBaseExclusivaLocal(){
        assertThatCode(()->comprobar("jdbc:mysql://127.0.0.1:3306/lotes_issue49_test?serverTimezone=UTC")).doesNotThrowAnyException();
        assertThatCode(()->comprobar("jdbc:mysql://localhost/lotes_issue49_test")).doesNotThrowAnyException();
    }
}
