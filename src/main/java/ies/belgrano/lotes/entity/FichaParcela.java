package ies.belgrano.lotes.entity;
import jakarta.persistence.*;
import java.time.Instant;

/** Aportes del equipo, separados de los atributos originales del catastro. */
@Entity @Table(name="fichas_parcelas")
public class FichaParcela {
    @Id @Column(length=100) private String parcelaId;
    @Column(length=255) private String direccion;
    @Column(length=2000) private String observaciones;
    @Column(nullable=false,length=255) private String fuente;
    @Column(nullable=false,length=1000) private String referencia;
    @Column(nullable=false,length=150) private String actualizadoPor;
    @Column(nullable=false) private Instant actualizadoEn;
    @Version private Long version;
    protected FichaParcela(){}
    public FichaParcela(String parcelaId){this.parcelaId=parcelaId;}
    public void actualizar(String direccion,String observaciones,String fuente,String referencia,String autor){
        this.direccion=direccion;this.observaciones=observaciones;this.fuente=fuente;
        this.referencia=referencia;this.actualizadoPor=autor;this.actualizadoEn=Instant.now();
    }
    public String getParcelaId(){return parcelaId;} public String getDireccion(){return direccion;}
    public String getObservaciones(){return observaciones;} public String getFuente(){return fuente;}
    public String getReferencia(){return referencia;} public String getActualizadoPor(){return actualizadoPor;}
    public Instant getActualizadoEn(){return actualizadoEn;} public Long getVersion(){return version;}
}
