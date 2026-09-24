package ies.belgrano.lotes.dto.request;
import jakarta.validation.constraints.*;
public record ProcedenciaRequest(@NotNull Tipo tipo,@NotBlank @Size(max=255) String fuente,
    @Size(max=1000) String referencia) {
    public enum Tipo { SIMULADO, NO_VERIFICADO, OFICIAL }
    @AssertTrue(message="Los datos oficiales requieren una referencia documental")
    public boolean isReferenciaValida() { return tipo!=Tipo.OFICIAL || referencia!=null && !referencia.isBlank(); }
}
