package ies.belgrano.lotes.dto.request;
import jakarta.validation.constraints.*;
public record ProcedenciaRequest(@NotNull(message="Elegí el tipo de procedencia") Tipo tipo,
    @NotBlank(message="Indicá la fuente de este dato") @Size(max=255,message="Usá como máximo 255 caracteres") String fuente,
    @Size(max=1000,message="Usá como máximo 1000 caracteres") String referencia) {
    public enum Tipo { SIMULADO, NO_VERIFICADO, OFICIAL }
    @AssertTrue(message="Agregá una referencia documental para los datos oficiales")
    public boolean isReferenciaValida() { return tipo!=Tipo.OFICIAL || referencia!=null && !referencia.isBlank(); }
}
