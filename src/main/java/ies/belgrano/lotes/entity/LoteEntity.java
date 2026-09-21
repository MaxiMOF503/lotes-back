package ies.belgrano.lotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "lotes")
public class LoteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String identificador;

	@Column(columnDefinition = "POINT SRID 4326", nullable = false)
	private Point ubicacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departamento_id")
	private DepartamentoEntity departamento;

	@Column(length = 255)
	private String direccionAproximada;

	@Column(length = 255)
	private String zonificacion;

	@Column(nullable = false)
	private boolean zonificacionVerificada;

	private Double distanciaRedElectricaMts;

	@Column(nullable = false)
	private boolean tieneAccesoElectricidad;

	@Column(nullable = false)
	private boolean tieneCoberturaAgua;

	@Column(length = 255)
	private String titularSimulado;

	@Column(length = 255)
	private String situacionDominialSimulada;

	@Column(nullable = false)
	private boolean esDatoSimulado = true;

	protected LoteEntity() {
	}

	public LoteEntity(
			String identificador,
			Point ubicacion,
			DepartamentoEntity departamento,
			String direccionAproximada,
			String zonificacion,
			boolean zonificacionVerificada,
			Double distanciaRedElectricaMts,
			boolean tieneAccesoElectricidad,
			boolean tieneCoberturaAgua,
			String titularSimulado,
			String situacionDominialSimulada,
			boolean esDatoSimulado) {
		this.identificador = identificador;
		this.ubicacion = ubicacion;
		this.departamento = departamento;
		this.direccionAproximada = direccionAproximada;
		this.zonificacion = zonificacion;
		this.zonificacionVerificada = zonificacionVerificada;
		this.distanciaRedElectricaMts = distanciaRedElectricaMts;
		this.tieneAccesoElectricidad = tieneAccesoElectricidad;
		this.tieneCoberturaAgua = tieneCoberturaAgua;
		this.titularSimulado = titularSimulado;
		this.situacionDominialSimulada = situacionDominialSimulada;
		this.esDatoSimulado = esDatoSimulado;
	}

	public String getIdentificador() {
		return identificador;
	}

	public Point getUbicacion() {
		return ubicacion;
	}

	public DepartamentoEntity getDepartamento() {
		return departamento;
	}

	public String getDireccionAproximada() {
		return direccionAproximada;
	}

	public String getZonificacion() {
		return zonificacion;
	}

	public boolean isZonificacionVerificada() {
		return zonificacionVerificada;
	}

	public Double getDistanciaRedElectricaMts() {
		return distanciaRedElectricaMts;
	}

	public boolean isTieneAccesoElectricidad() {
		return tieneAccesoElectricidad;
	}

	public boolean isTieneCoberturaAgua() {
		return tieneCoberturaAgua;
	}

	public String getTitularSimulado() {
		return titularSimulado;
	}

	public String getSituacionDominialSimulada() {
		return situacionDominialSimulada;
	}

	public boolean isEsDatoSimulado() {
		return esDatoSimulado;
	}
}
