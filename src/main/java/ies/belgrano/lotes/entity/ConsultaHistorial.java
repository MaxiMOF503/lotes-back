package ies.belgrano.lotes.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="consultas_historial", indexes=@Index(name="idx_historial_usuario_fecha", columnList="usuario_id,consultada_en"))
public class ConsultaHistorial {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="usuario_id",nullable=false) private UsuarioEntity usuario;
    @Column(nullable=false) private double latitud;
    @Column(nullable=false) private double longitud;
    @Column(name="consultada_en",nullable=false) private Instant consultadaEn;
    @Column(nullable=false,length=800) private String resumen;
    @Lob @Column(nullable=false,columnDefinition="LONGTEXT") private String snapshot;
    protected ConsultaHistorial() {}
    public ConsultaHistorial(UsuarioEntity usuario,double latitud,double longitud,Instant fecha,String resumen,String snapshot){
        this.usuario=usuario;this.latitud=latitud;this.longitud=longitud;this.consultadaEn=fecha;this.resumen=resumen;this.snapshot=snapshot;
    }
    public Long getId(){return id;} public double getLatitud(){return latitud;}
    public double getLongitud(){return longitud;} public Instant getConsultadaEn(){return consultadaEn;}
    public String getResumen(){return resumen;} public String getSnapshot(){return snapshot;}
}
