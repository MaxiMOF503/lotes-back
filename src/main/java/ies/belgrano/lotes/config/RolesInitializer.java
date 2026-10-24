package ies.belgrano.lotes.config;

import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.repository.RolRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Garantiza los dos roles del sistema. No crea cuentas ni datos de ejemplo. */
@Component
public class RolesInitializer implements ApplicationRunner {
	private final RolRepository roles;

	public RolesInitializer(RolRepository roles) {
		this.roles = roles;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (RolEntity.NombreRol nombre : RolEntity.NombreRol.values()) {
			if (roles.findByNombre(nombre).isEmpty()) roles.save(new RolEntity(nombre));
		}
	}
}
