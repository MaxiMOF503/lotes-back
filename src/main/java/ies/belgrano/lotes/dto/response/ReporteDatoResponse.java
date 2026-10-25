package ies.belgrano.lotes.dto.response;

import ies.belgrano.lotes.entity.ReporteDato;
import java.time.Instant;

public record ReporteDatoResponse(Long id, String parcelaId, String nomenclatura,
        ReporteDato.Seccion seccion, ReporteDato.Tipo tipo, String descripcion,
        ReporteDato.Estado estado, String respuesta, Persona creadoPor, Persona respondidoPor,
        Instant creadoEn, Instant actualizadoEn, Instant respondidoEn,String parcelaIdCanonico) {
    public ReporteDatoResponse(Long id,String parcelaId,String nomenclatura,ReporteDato.Seccion seccion,
            ReporteDato.Tipo tipo,String descripcion,ReporteDato.Estado estado,String respuesta,Persona creadoPor,
            Persona respondidoPor,Instant creadoEn,Instant actualizadoEn,Instant respondidoEn){
        this(id,parcelaId,nomenclatura,seccion,tipo,descripcion,estado,respuesta,creadoPor,respondidoPor,
            creadoEn,actualizadoEn,respondidoEn,null);
    }
    public record Persona(String nombre, String email) {}
}
