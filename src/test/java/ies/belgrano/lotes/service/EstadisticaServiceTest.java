package ies.belgrano.lotes.service;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
class EstadisticaServiceTest {
    @Test void unaFallaEstadisticaNoSePropaga() {
        var manager=mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenThrow(new org.springframework.transaction.CannotCreateTransactionException("Base no disponible"));
        var service=new EstadisticaService(mock(org.springframework.jdbc.core.JdbcTemplate.class),manager,"America/Argentina/Buenos_Aires");
        assertDoesNotThrow(() -> service.registrar("CONSULTA","IDENTIFICADOR","EXITO"));
    }
}
