package ies.belgrano.lotes.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="consultas_guardadas", uniqueConstraints=@UniqueConstraint(columnNames={"usuario_id","parcela_id"}))
public class ConsultaGuardada {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="usuario_id",nullable=false) private UsuarioEntity usuario;
    @Column(name="parcela_id",nullable=false,length=100) private String parcelaId;
    @Column(nullable=false,length=150) private String nomenclatura;
    @Column(length=100) private String etiqueta;
    @Column(nullable=false) private double latitud;
    @Column(nullable=false) private double longitud;
    @Column(nullable=false) private Instant creadaEn;
    protected ConsultaGuardada() {}
    public ConsultaGuardada(UsuarioEntity usuario,String parcelaId,String nomenclatura) {
        this.usuario=usuario;this.parcelaId=parcelaId;this.nomenclatura=nomenclatura;this.creadaEn=Instant.now();
    }
    public void actualizar(double lat,double lon,String etiqueta){this.latitud=lat;this.longitud=lon;this.etiqueta=etiqueta;}
    public Long getId(){return id;} public String getParcelaId(){return parcelaId;}
    public String getNomenclatura(){return nomenclatura;} public String getEtiqueta(){return etiqueta;}
    public double getLatitud(){return latitud;} public double getLongitud(){return longitud;}
    public Instant getCreadaEn(){return creadaEn;}
}
