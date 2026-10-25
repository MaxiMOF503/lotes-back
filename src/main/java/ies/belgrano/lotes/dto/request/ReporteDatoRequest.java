package ies.belgrano.lotes.dto.request;

import ies.belgrano.lotes.entity.ReporteDato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReporteDatoRequest {
    private ReporteDatoRequest() {}

    public record Crear(@NotBlank @Size(max = 100) String parcelaId,
        @NotNull ReporteDato.Seccion seccion, @NotNull ReporteDato.Tipo tipo,
        @NotBlank @Size(min = 10, max = 1000) String descripcion) {}

    public record Actualizar(@NotNull ReporteDato.Estado estado,
        @Size(max = 1000) String respuesta) {}
}
