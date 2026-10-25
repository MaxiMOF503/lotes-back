package ies.belgrano.lotes.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reportes_datos", indexes = {
    @Index(name = "idx_reporte_usuario_fecha", columnList = "creado_por_id,creado_en"),
    @Index(name = "idx_reporte_estado_fecha", columnList = "estado,creado_en"),
    @Index(name = "idx_reporte_parcela", columnList = "parcela_id")
})
public class ReporteDato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "parcela_id", nullable = false, length = 100) private String parcelaId;
    @Column(nullable = false, length = 150) private String nomenclatura;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Seccion seccion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Tipo tipo;
    @Column(nullable = false, length = 1000) private String descripcion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Estado estado;
    @Column(length = 1000) private String respuesta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por_id", nullable = false) private UsuarioEntity creadoPor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondido_por_id") private UsuarioEntity respondidoPor;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    @Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;
    @Column(name = "respondido_en") private Instant respondidoEn;

    protected ReporteDato() {}

    public ReporteDato(UsuarioEntity creadoPor, String parcelaId, String nomenclatura,
            Seccion seccion, Tipo tipo, String descripcion, Instant fecha) {
        this.creadoPor = creadoPor;
        this.parcelaId = parcelaId;
        this.nomenclatura = nomenclatura;
        this.seccion = seccion;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.estado = Estado.PENDIENTE;
        this.creadoEn = fecha;
        this.actualizadoEn = fecha;
    }

    public void actualizar(Estado estado, String respuesta, UsuarioEntity administrador, Instant fecha) {
        this.estado = estado;
        this.respuesta = respuesta;
        this.actualizadoEn = fecha;
        if (respuesta != null || estado == Estado.RESUELTO || estado == Estado.DESCARTADO) {
            this.respondidoPor = administrador;
            this.respondidoEn = fecha;
        } else {
            this.respondidoPor = null;
            this.respondidoEn = null;
        }
    }

    public Long getId() { return id; }
    public String getParcelaId() { return parcelaId; }
    public String getNomenclatura() { return nomenclatura; }
    public Seccion getSeccion() { return seccion; }
    public Tipo getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public Estado getEstado() { return estado; }
    public String getRespuesta() { return respuesta; }
    public UsuarioEntity getCreadoPor() { return creadoPor; }
    public UsuarioEntity getRespondidoPor() { return respondidoPor; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public Instant getRespondidoEn() { return respondidoEn; }

    public enum Seccion { PARCELA, ZONIFICACION, AGUA, ELECTRICIDAD, FICHA_COMPLEMENTARIA, OTRO }
    public enum Tipo { INCORRECTO, FALTANTE }
    public enum Estado { PENDIENTE, EN_REVISION, RESUELTO, DESCARTADO }
}
