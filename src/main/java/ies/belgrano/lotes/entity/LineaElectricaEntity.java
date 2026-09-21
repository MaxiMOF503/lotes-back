package ies.belgrano.lotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.MultiLineString;

@Entity
@Table(name = "lineas_electricas")
public class LineaElectricaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(columnDefinition = "MULTILINESTRING SRID 4326", nullable = false)
	private MultiLineString trazado;

	@Column(length = 50)
	private String tipo;

	@Column(length = 255)
	private String fuenteDatos;

	protected LineaElectricaEntity() {
	}

	public LineaElectricaEntity(MultiLineString trazado, String tipo, String fuenteDatos) {
		this.trazado = trazado;
		this.tipo = tipo;
		this.fuenteDatos = fuenteDatos;
	}

	public MultiLineString getTrazado() {
		return trazado;
	}

	public String getTipo() {
		return tipo;
	}

	public String getFuenteDatos() {
		return fuenteDatos;
	}
}
