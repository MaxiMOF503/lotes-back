package ies.belgrano.lotes.dto.request;
import jakarta.validation.constraints.NotNull;
public record ConsultaPersonalRequest(@NotNull Double latitud,@NotNull Double longitud) {}
