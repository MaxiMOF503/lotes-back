package ies.belgrano.lotes.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.util.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
@Service
public class EstadisticaService {
    private final JdbcTemplate jdbc; private final ZoneId zona;
    private final org.springframework.transaction.support.TransactionTemplate transaccion;
    public EstadisticaService(JdbcTemplate jdbc,org.springframework.transaction.PlatformTransactionManager manager,
        @Value("${estadisticas.zona:America/Argentina/Buenos_Aires}") String zona) {
        this.jdbc=jdbc;this.zona=ZoneId.of(zona);
        transaccion=new org.springframework.transaction.support.TransactionTemplate(manager);
        transaccion.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaccion.setTimeout(2);
    }
    public void registrar(String tipo,String criterio,String resultado) {
        try {
            LocalDate fecha=LocalDate.now(zona);
            String clave=fecha+":"+tipo+":"+criterio+":"+resultado;
            transaccion.executeWithoutResult(s -> jdbc.update("""
                INSERT INTO estadisticas_diarias (clave,fecha,tipo,criterio,resultado,cantidad) VALUES (?,?,?,?,?,1)
                ON DUPLICATE KEY UPDATE cantidad=cantidad+1
                """,clave,fecha,tipo,criterio,resultado));
        } catch(RuntimeException e) {
            LoggerFactory.getLogger(getClass()).warn("No se pudo registrar una estadística; la consulta continúa");
        }
    }
    public record Dia(LocalDate fecha,long visitas,long consultas,long porIdentificador,long porCoordenadas,long sinCriterioValido,
        long exitosas,long invalidas,long sinResultados,long erroresTecnicos) {}
    public record Resumen(LocalDate desde,LocalDate hasta,String zona,long visitas,long consultas,List<Dia> dias) {}
    public Resumen consultar(LocalDate desde,LocalDate hasta) {
        if(desde==null || hasta==null || hasta.isBefore(desde) || java.time.temporal.ChronoUnit.DAYS.between(desde,hasta)>365)
            throw new OperacionInvalidaException(400,"RANGO_INVALIDO","Informar desde y hasta en orden, con un máximo de 366 días");
        var filas=jdbc.queryForList("SELECT fecha,tipo,criterio,resultado,cantidad FROM estadisticas_diarias WHERE fecha BETWEEN ? AND ?",desde,hasta);
        Map<LocalDate,long[]> contadores=new TreeMap<>();
        for(LocalDate d=desde;!d.isAfter(hasta);d=d.plusDays(1)) contadores.put(d,new long[9]);
        for(var fila:filas) {
            var fecha=((java.sql.Date)fila.get("fecha")).toLocalDate();var c=contadores.get(fecha);long n=((Number)fila.get("cantidad")).longValue();
            if("VISITA".equals(fila.get("tipo"))) {c[0]+=n;continue;}
            c[1]+=n;
            int criterio=switch(fila.get("criterio").toString()) {case "IDENTIFICADOR" -> 2;case "COORDENADAS" -> 3;default -> 4;};c[criterio]+=n;
            int resultado=switch(fila.get("resultado").toString()) {case "EXITO" -> 5;case "INVALIDA" -> 6;case "SIN_RESULTADOS" -> 7;default -> 8;};c[resultado]+=n;
        }
        var dias=contadores.entrySet().stream().map(e -> {var c=e.getValue();return new Dia(e.getKey(),c[0],c[1],c[2],c[3],c[4],c[5],c[6],c[7],c[8]);}).toList();
        return new Resumen(desde,hasta,zona.toString(),dias.stream().mapToLong(Dia::visitas).sum(),dias.stream().mapToLong(Dia::consultas).sum(),dias);
    }
}
