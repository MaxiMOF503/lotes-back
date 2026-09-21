package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.AguaResponse;
import ies.belgrano.lotes.dto.response.CoordenadasResponse;
import ies.belgrano.lotes.dto.response.CriterioConsultaResponse;
import ies.belgrano.lotes.dto.response.DatosLoteResponse;
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
				PROCEDENCIA_SIMULADA);

		ZonificacionResponse zonificacion = new ZonificacionResponse(
				lote.getZonificacion(),
				lote.isZonificacionVerificada(),
				PROCEDENCIA_SIMULADA);

		ElectricidadResponse electricidad = new ElectricidadResponse(
				lote.getDistanciaRedElectricaMts(),
				lote.isTieneAccesoElectricidad(),
				PROCEDENCIA_SIMULADA);

		AguaResponse agua = new AguaResponse(
				lote.isTieneCoberturaAgua(),
				PROCEDENCIA_SIMULADA);

		return new FichaLoteResponse(
				criterioResponse(criteria),
				datosLote,
				zonificacion,
				electricidad,
				agua,
				ADVERTENCIA);
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
