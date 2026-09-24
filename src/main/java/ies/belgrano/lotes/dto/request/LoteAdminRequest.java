package ies.belgrano.lotes.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record LoteAdminRequest(
    @NotBlank @Size(max=100) String identificador,
    @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
    @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud,
    @Positive Long departamentoId,@Size(max=255) String direccionAproximada,
    @Size(max=255) String zonificacion,@NotNull Boolean zonificacionVerificada,
    @DecimalMin("0") Double distanciaRedElectricaMts,
    @NotNull Boolean tieneAccesoElectricidad,@NotNull Boolean tieneCoberturaAgua,
    @NotNull @Valid ProcedenciaRequest procedenciaLote,
    @NotNull @Valid ProcedenciaRequest procedenciaZonificacion,
    @NotNull @Valid ProcedenciaRequest procedenciaElectricidad,
    @NotNull @Valid ProcedenciaRequest procedenciaAgua) {
    @AssertTrue(message="La distancia debe ser un número finito")
    public boolean isDistanciaFinita() { return distanciaRedElectricaMts==null || Double.isFinite(distanciaRedElectricaMts); }
    @AssertTrue(message="Para verificar zonificación se requiere descripción y fuente oficial")
    public boolean isZonificacionConsistente() { return !Boolean.TRUE.equals(zonificacionVerificada) ||
        zonificacion!=null && !zonificacion.isBlank() && procedenciaZonificacion!=null && procedenciaZonificacion.tipo()==ProcedenciaRequest.Tipo.OFICIAL; }
}
