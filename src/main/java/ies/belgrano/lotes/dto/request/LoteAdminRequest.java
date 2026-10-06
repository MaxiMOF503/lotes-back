package ies.belgrano.lotes.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record LoteAdminRequest(
    @NotBlank(message="Ingresá un identificador") @Size(max=100,message="Usá como máximo 100 caracteres") String identificador,
    @NotNull(message="Ingresá la latitud") @DecimalMin(value="-90",message="La latitud debe estar entre -90 y 90") @DecimalMax(value="90",message="La latitud debe estar entre -90 y 90") BigDecimal latitud,
    @NotNull(message="Ingresá la longitud") @DecimalMin(value="-180",message="La longitud debe estar entre -180 y 180") @DecimalMax(value="180",message="La longitud debe estar entre -180 y 180") BigDecimal longitud,
    @Positive(message="Elegí un departamento válido") Long departamentoId,@Size(max=255,message="Usá como máximo 255 caracteres") String direccionAproximada,
    @Size(max=255,message="Usá como máximo 255 caracteres") String zonificacion,@NotNull(message="Indicá si la zonificación está verificada") Boolean zonificacionVerificada,
    @DecimalMin(value="0",message="La distancia no puede ser negativa") Double distanciaRedElectricaMts,
    @NotNull(message="Indicá si tiene electricidad") Boolean tieneAccesoElectricidad,@NotNull(message="Indicá si tiene cobertura de agua") Boolean tieneCoberturaAgua,
    @NotNull(message="Completá la procedencia del lote") @Valid ProcedenciaRequest procedenciaLote,
    @NotNull(message="Completá la procedencia de zonificación") @Valid ProcedenciaRequest procedenciaZonificacion,
    @NotNull(message="Completá la procedencia de electricidad") @Valid ProcedenciaRequest procedenciaElectricidad,
    @NotNull(message="Completá la procedencia de agua") @Valid ProcedenciaRequest procedenciaAgua) {
    @AssertTrue(message="Ingresá una distancia numérica válida")
    public boolean isDistanciaFinita() { return distanciaRedElectricaMts==null || Double.isFinite(distanciaRedElectricaMts); }
    @AssertTrue(message="Para marcar la zonificación como verificada, describila y elegí procedencia Oficial")
    public boolean isZonificacionConsistente() { return !Boolean.TRUE.equals(zonificacionVerificada) ||
        zonificacion!=null && !zonificacion.isBlank() && procedenciaZonificacion!=null && procedenciaZonificacion.tipo()==ProcedenciaRequest.Tipo.OFICIAL; }
}
