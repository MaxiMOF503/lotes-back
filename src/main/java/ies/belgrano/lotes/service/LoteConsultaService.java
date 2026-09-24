package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.AguaResponse;
import ies.belgrano.lotes.dto.response.CoordenadasResponse;
import ies.belgrano.lotes.dto.response.CriterioConsultaResponse;
import ies.belgrano.lotes.dto.response.DatosLoteResponse;
import ies.belgrano.lotes.dto.response.DondeConsultarResponse;
import ies.belgrano.lotes.dto.response.ElectricidadResponse;
import ies.belgrano.lotes.dto.response.FichaLoteResponse;
import ies.belgrano.lotes.dto.response.ProcedenciaResponse;
import ies.belgrano.lotes.dto.response.ZonificacionResponse;
import ies.belgrano.lotes.entity.LoteEntity;
import ies.belgrano.lotes.exception.LoteNoEncontradoException;
import ies.belgrano.lotes.model.ConsultaLoteCriteria;
import ies.belgrano.lotes.repository.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class LoteConsultaService {

	public static final String ADVERTENCIA = "La información de esta ficha es demostrativa y no constituye "
			+ "información catastral, municipal ni legal oficial.";

	private static final ProcedenciaResponse PROCEDENCIA_SIMULADA =
			new ProcedenciaResponse("SIMULADO", "Dataset demostrativo B-01", false);

	private final LoteRepository loteRepository;

	public LoteConsultaService(LoteRepository loteRepository) {
		this.loteRepository = loteRepository;
	}

	@Transactional(readOnly = true)
	public FichaLoteResponse consultar(ConsultaLoteCriteria criteria) {
		LoteEntity lote = buscar(criteria);

		DatosLoteResponse datosLote = new DatosLoteResponse(
				lote.getIdentificador(),
				lote.getDireccionAproximada(),
				lote.getDepartamento() != null ? lote.getDepartamento().getNombre() : null,
				new CoordenadasResponse(
						BigDecimal.valueOf(lote.getUbicacion().getY()),
						BigDecimal.valueOf(lote.getUbicacion().getX())),
				procedencia(lote.getProcedenciaLote()));

		ZonificacionResponse zonificacion = new ZonificacionResponse(
				lote.getZonificacion(),
				lote.isZonificacionVerificada(),
				procedencia(lote.getProcedenciaZonificacion()));

		ElectricidadResponse electricidad = new ElectricidadResponse(
				lote.getDistanciaRedElectricaMts(),
				lote.isTieneAccesoElectricidad(),
				procedencia(lote.getProcedenciaElectricidad()));

		AguaResponse agua = new AguaResponse(
				lote.isTieneCoberturaAgua(),
				procedencia(lote.getProcedenciaAgua()));

		return new FichaLoteResponse(
				criterioResponse(criteria),
				datosLote,
				zonificacion,
				electricidad,
				agua,
				advertencia(lote),
				dondeConsultar(lote));
	}

    private ProcedenciaResponse procedencia(ies.belgrano.lotes.entity.ProcedenciaDatos datos) {
        return datos==null ? PROCEDENCIA_SIMULADA : datos.response();
    }
    private String advertencia(LoteEntity lote) {
        var datos=java.util.Arrays.asList(lote.getProcedenciaLote(),lote.getProcedenciaZonificacion(),lote.getProcedenciaElectricidad(),lote.getProcedenciaAgua());
        if(datos.stream().allMatch(p -> p==null || "SIMULADO".equals(p.response().tipo()))) return ADVERTENCIA;
        return "Consulte la procedencia de cada sección. La ficha es orientativa y no reemplaza la verificación ante el organismo competente.";
    }
	private DondeConsultarResponse dondeConsultar(LoteEntity lote) {
		var departamento = lote.getDepartamento();
		if (departamento == null) {
			return null;
		}
		return new DondeConsultarResponse(
				departamento.getNombre(),
				departamento.getContactoOficial(),
				departamento.getDireccionOficina(),
				departamento.getComoConsultar(),
				new ProcedenciaResponse("SIMULADO", departamento.getFuenteDatos(), false));
	}

	private LoteEntity buscar(ConsultaLoteCriteria criteria) {
		return (switch (criteria.tipo()) {
			case IDENTIFICADOR -> loteRepository.findByIdentificador(criteria.identificador());
			case COORDENADAS -> loteRepository.findByCoordenadasExactas(
					criteria.latitud(), criteria.longitud());
		}).orElseThrow(LoteNoEncontradoException::new);
	}

	private CriterioConsultaResponse criterioResponse(ConsultaLoteCriteria criteria) {
		return new CriterioConsultaResponse(
				criteria.tipo().name(),
				criteria.identificador(),
				criteria.latitud(),
				criteria.longitud());
	}
}
