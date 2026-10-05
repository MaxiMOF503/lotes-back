package ies.belgrano.lotes.dto.request;

import ies.belgrano.lotes.model.ConsultaLoteCriteria;
import ies.belgrano.lotes.validation.ConsultaLoteValida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@ConsultaLoteValida
public class ConsultaLoteRequest {

	@Size(max = 100, message = "El identificador no puede superar los 100 caracteres")
	@Pattern(regexp = ".*\\S.*", message = "El identificador no puede estar vacío")
	@Schema(example = "LOT-DEMO-001", description = "Identificador público del lote")
	private String identificador;

	@Size(min = 3, max = 255, message = "La dirección debe tener entre 3 y 255 caracteres")
	@Pattern(regexp = "[^%_]*\\S[^%_]*", message = "La dirección no puede estar vacía ni contener % o _")
	@Schema(example = "Calle Demostración 123, Mendoza", description = "Dirección aproximada del lote")
	private String direccion;

	@DecimalMin(value = "-90", message = "La latitud debe ser mayor o igual a -90")
	@DecimalMax(value = "90", message = "La latitud debe ser menor o igual a 90")
	@Schema(example = "-32.8895", description = "Latitud WGS84")
	private BigDecimal latitud;

	@DecimalMin(value = "-180", message = "La longitud debe ser mayor o igual a -180")
	@DecimalMax(value = "180", message = "La longitud debe ser menor o igual a 180")
	@Schema(example = "-68.8458", description = "Longitud WGS84")
	private BigDecimal longitud;

	public String getIdentificador() {
		return identificador;
	}

	public void setIdentificador(String identificador) {
		this.identificador = identificador;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion == null ? null : direccion.trim().replaceAll("\\s+", " ");
	}

	public BigDecimal getLatitud() {
		return latitud;
	}

	public void setLatitud(BigDecimal latitud) {
		this.latitud = latitud;
	}

	public BigDecimal getLongitud() {
		return longitud;
	}

	public void setLongitud(BigDecimal longitud) {
		this.longitud = longitud;
	}

	public ConsultaLoteCriteria toCriteria() {
		if (identificador != null && !identificador.isBlank()) {
			return ConsultaLoteCriteria.porIdentificador(identificador.trim());
		}
		if (direccion != null && !direccion.isBlank()) {
			return ConsultaLoteCriteria.porDireccion(direccion);
		}
		return ConsultaLoteCriteria.porCoordenadas(latitud, longitud);
	}
}
