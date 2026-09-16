package ies.belgrano.lotes.dto.response;

public record ElectricidadResponse(
		Double distanciaRedElectricaMts,
		boolean tieneAcceso,
		ProcedenciaResponse procedencia) {
}
