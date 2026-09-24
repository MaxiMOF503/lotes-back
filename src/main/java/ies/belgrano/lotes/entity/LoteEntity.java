package ies.belgrano.lotes.entity;

import jakarta.persistence.*;
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


    @Embedded @AttributeOverrides({
        @AttributeOverride(name="tipo",column=@Column(name="lote_tipo")),
        @AttributeOverride(name="fuente",column=@Column(name="lote_fuente")),
        @AttributeOverride(name="referencia",column=@Column(name="lote_referencia",length=1000))})
    private ProcedenciaDatos procedenciaLote;
    public ProcedenciaDatos getProcedenciaLote() { return procedenciaLote; }

    @Embedded @AttributeOverrides({
        @AttributeOverride(name="tipo",column=@Column(name="zonificacion_tipo")),
        @AttributeOverride(name="fuente",column=@Column(name="zonificacion_fuente")),
        @AttributeOverride(name="referencia",column=@Column(name="zonificacion_referencia",length=1000))})
    private ProcedenciaDatos procedenciaZonificacion;
    public ProcedenciaDatos getProcedenciaZonificacion() { return procedenciaZonificacion; }

    @Embedded @AttributeOverrides({
        @AttributeOverride(name="tipo",column=@Column(name="electricidad_tipo")),
        @AttributeOverride(name="fuente",column=@Column(name="electricidad_fuente")),
        @AttributeOverride(name="referencia",column=@Column(name="electricidad_referencia",length=1000))})
    private ProcedenciaDatos procedenciaElectricidad;
    public ProcedenciaDatos getProcedenciaElectricidad() { return procedenciaElectricidad; }

    @Embedded @AttributeOverrides({
        @AttributeOverride(name="tipo",column=@Column(name="agua_tipo")),
        @AttributeOverride(name="fuente",column=@Column(name="agua_fuente")),
        @AttributeOverride(name="referencia",column=@Column(name="agua_referencia",length=1000))})
    private ProcedenciaDatos procedenciaAgua;
    public ProcedenciaDatos getProcedenciaAgua() { return procedenciaAgua; }

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

    public Long getId() { return id; }
    public void actualizar(ies.belgrano.lotes.dto.request.LoteAdminRequest r, DepartamentoEntity departamento) {
        identificador=r.identificador().trim();
        ubicacion=new org.locationtech.jts.geom.GeometryFactory(new org.locationtech.jts.geom.PrecisionModel(),4326)
            .createPoint(new org.locationtech.jts.geom.Coordinate(r.longitud().doubleValue(),r.latitud().doubleValue()));
        this.departamento=departamento;direccionAproximada=limpiar(r.direccionAproximada());zonificacion=limpiar(r.zonificacion());
        zonificacionVerificada=r.zonificacionVerificada();distanciaRedElectricaMts=r.distanciaRedElectricaMts();
        tieneAccesoElectricidad=r.tieneAccesoElectricidad();tieneCoberturaAgua=r.tieneCoberturaAgua();
        procedenciaLote=new ProcedenciaDatos(r.procedenciaLote());procedenciaZonificacion=new ProcedenciaDatos(r.procedenciaZonificacion());
        procedenciaElectricidad=new ProcedenciaDatos(r.procedenciaElectricidad());procedenciaAgua=new ProcedenciaDatos(r.procedenciaAgua());
        esDatoSimulado=r.procedenciaLote().tipo()==ies.belgrano.lotes.dto.request.ProcedenciaRequest.Tipo.SIMULADO;
    }
    public static LoteEntity crear(ies.belgrano.lotes.dto.request.LoteAdminRequest r, DepartamentoEntity departamento) {
        var lote=new LoteEntity();lote.actualizar(r,departamento);return lote;
    }
    private static String limpiar(String valor) { return valor==null || valor.isBlank() ? null : valor.trim(); }

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
