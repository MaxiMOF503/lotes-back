package ies.belgrano.lotes.dto.request;
import jakarta.validation.constraints.*;

public final class EspacioTerritorialRequest {
    private EspacioTerritorialRequest(){}
    public record Guardar(@NotNull Double latitud,@NotNull Double longitud,@Size(max=100) String etiqueta){}
    public record Ficha(@Size(max=255) String direccion,@Size(max=2000) String observaciones,
                        @NotBlank @Size(max=255) String fuente,@NotBlank @Size(max=1000) String referencia,
                        @PositiveOrZero Long version){}
}
