package ies.belgrano.lotes.dto.response;

import ies.belgrano.lotes.entity.ReporteDato;
import java.time.Instant;

public record ReporteDatoResponse(Long id, String parcelaId, String nomenclatura,
        ReporteDato.Seccion seccion, ReporteDato.Tipo tipo, String descripcion,
        ReporteDato.Estado estado, String respuesta, Persona creadoPor, Persona respondidoPor,
        Instant creadoEn, Instant actualizadoEn, Instant respondidoEn) {
    public record Persona(String nombre, String email) {}
}
