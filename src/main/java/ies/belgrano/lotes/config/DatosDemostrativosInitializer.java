package ies.belgrano.lotes.config;

import ies.belgrano.lotes.entity.DepartamentoEntity;
import ies.belgrano.lotes.entity.LineaElectricaEntity;
import ies.belgrano.lotes.entity.LoteEntity;
import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.repository.DepartamentoRepository;
import ies.belgrano.lotes.repository.LineaElectricaRepository;
import ies.belgrano.lotes.repository.LoteRepository;
import ies.belgrano.lotes.repository.RolRepository;
import ies.belgrano.lotes.repository.UsuarioRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatosDemostrativosInitializer implements ApplicationRunner {

	private static final String IDENTIFICADOR_DEMO = "LOT-DEMO-001";
	private static final String DEPARTAMENTO_DEMO = "Luján de Cuyo";
	private static final String EMAIL_USUARIO_DEMO = "usuario.demo@loteseguro.invalid";
	private static final String PASSWORD_NO_OPERATIVO = "NO-OPERATIVA-DEMO-B01-SIN-CREDENCIAL";
	private static final String TIPO_LINEA_DEMO = "RED DEMOSTRATIVA";
	private static final String FUENTE_DEMO = "Dataset demostrativo B-01";
	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	private final LoteRepository loteRepository;
	private final DepartamentoRepository departamentoRepository;
	private final RolRepository rolRepository;
	private final UsuarioRepository usuarioRepository;
	private final LineaElectricaRepository lineaElectricaRepository;

	public DatosDemostrativosInitializer(
			LoteRepository loteRepository,
			DepartamentoRepository departamentoRepository,
			RolRepository rolRepository,
			UsuarioRepository usuarioRepository,
			LineaElectricaRepository lineaElectricaRepository) {
		this.loteRepository = loteRepository;
		this.departamentoRepository = departamentoRepository;
		this.rolRepository = rolRepository;
		this.usuarioRepository = usuarioRepository;
		this.lineaElectricaRepository = lineaElectricaRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		obtenerOCrearRol(RolEntity.NombreRol.ADMIN);
		RolEntity rolUsuario = obtenerOCrearRol(RolEntity.NombreRol.USUARIO);

		if (!usuarioRepository.existsByEmail(EMAIL_USUARIO_DEMO)) {
			usuarioRepository.save(new UsuarioEntity(
					"Usuario demostrativo no operativo",
					EMAIL_USUARIO_DEMO,
					PASSWORD_NO_OPERATIVO,
					rolUsuario));
		}

		DepartamentoEntity departamento = departamentoRepository.findByNombre(DEPARTAMENTO_DEMO)
				.orElseGet(() -> departamentoRepository.save(new DepartamentoEntity(
						DEPARTAMENTO_DEMO,
						"Contacto demostrativo no oficial",
						"Oficina demostrativa",
						"Consultar la zonificación ante el organismo municipal competente",
						"Dataset demostrativo B-01")));

		if (!loteRepository.existsByIdentificador(IDENTIFICADOR_DEMO)) {
			Point ubicacion = GEOMETRY_FACTORY.createPoint(new Coordinate(-68.8458, -32.8895));

			LoteEntity lote = new LoteEntity(
					IDENTIFICADOR_DEMO,
					ubicacion,
					departamento,
					"Calle Demostración 123, Mendoza",
					"Residencial R2 (dato simulado)",
					false,
					320.0,
					true,
					true,
					"Titular demostrativo no oficial",
					"Situación dominial simulada sin valor legal",
					true);

			loteRepository.save(lote);
		}

		if (!lineaElectricaRepository.existsByTipoAndFuenteDatos(TIPO_LINEA_DEMO, FUENTE_DEMO)) {
			lineaElectricaRepository.save(new LineaElectricaEntity(
					crearTrazadoElectricoDemostrativo(),
					TIPO_LINEA_DEMO,
					FUENTE_DEMO));
		}
	}

	private RolEntity obtenerOCrearRol(RolEntity.NombreRol nombre) {
		return rolRepository.findByNombre(nombre)
				.orElseGet(() -> rolRepository.save(new RolEntity(nombre)));
	}

	private MultiLineString crearTrazadoElectricoDemostrativo() {
		Coordinate[] coordenadas = {
				new Coordinate(-68.8480, -32.8905),
				new Coordinate(-68.8465, -32.8899),
				new Coordinate(-68.8445, -32.8888)
		};
		LineString linea = GEOMETRY_FACTORY.createLineString(coordenadas);
		return GEOMETRY_FACTORY.createMultiLineString(new LineString[]{linea});
	}
}
