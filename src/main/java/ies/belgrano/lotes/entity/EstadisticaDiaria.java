package ies.belgrano.lotes.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="estadisticas_diarias",indexes=@Index(name="idx_estadistica_fecha",columnList="fecha"))
public class EstadisticaDiaria {
    @Id @Column(length=100) private String clave;
    @Column(nullable=false) private LocalDate fecha;
    @Column(nullable=false,length=20) private String tipo;
    @Column(nullable=false,length=20) private String criterio;
    @Column(nullable=false,length=20) private String resultado;
    @Column(nullable=false) private long cantidad;
    protected EstadisticaDiaria() {}
}
