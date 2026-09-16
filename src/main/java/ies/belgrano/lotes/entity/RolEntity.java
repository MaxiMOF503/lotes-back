package ies.belgrano.lotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RolEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true, length = 20)
	private NombreRol nombre;

	protected RolEntity() {
	}

	public RolEntity(NombreRol nombre) {
		this.nombre = nombre;
	}

	public NombreRol getNombre() {
		return nombre;
	}

	public enum NombreRol {
		ADMIN,
		USUARIO
	}
}
