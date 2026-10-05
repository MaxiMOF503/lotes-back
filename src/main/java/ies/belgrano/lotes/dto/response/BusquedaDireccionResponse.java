package ies.belgrano.lotes.dto.response;

import java.util.List;

public record BusquedaDireccionResponse(List<Opcion> opciones, boolean hayMas) {
	public record Opcion(String identificador, String direccionAproximada, String departamento, boolean datoSimulado) {
	}
}
