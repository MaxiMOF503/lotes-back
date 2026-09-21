package ies.belgrano.lotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "departamentos")
public class DepartamentoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String nombre;

	@Column(length = 255)
	private String contactoOficial;

	@Column(length = 255)
	private String direccionOficina;

	@Column(length = 1000)
	private String comoConsultar;

	@Column(length = 255)
	private String fuenteDatos;

	protected DepartamentoEntity() {
	}

	public DepartamentoEntity(
			String nombre,
			String contactoOficial,
			String direccionOficina,
			String comoConsultar,
			String fuenteDatos) {
		this.nombre = nombre;
		this.contactoOficial = contactoOficial;
		this.direccionOficina = direccionOficina;
		this.comoConsultar = comoConsultar;
		this.fuenteDatos = fuenteDatos;
	}

	public String getNombre() {
		return nombre;
	}

	public String getContactoOficial() {
		return contactoOficial;
	}

	public String getDireccionOficina() {
		return direccionOficina;
	}

	public String getComoConsultar() {
		return comoConsultar;
	}

	public String getFuenteDatos() {
		return fuenteDatos;
	}
}
