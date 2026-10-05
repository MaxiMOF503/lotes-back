package ies.belgrano.lotes.validation;

import ies.belgrano.lotes.dto.request.ConsultaLoteRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConsultaLoteValidator implements ConstraintValidator<ConsultaLoteValida, ConsultaLoteRequest> {

	@Override
	public boolean isValid(ConsultaLoteRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}

		boolean tieneIdentificador = request.getIdentificador() != null
				&& !request.getIdentificador().isBlank();
		boolean tieneDireccion = request.getDireccion() != null
				&& !request.getDireccion().isBlank();
		boolean tieneLatitud = request.getLatitud() != null;
		boolean tieneLongitud = request.getLongitud() != null;

		return (tieneIdentificador && !tieneDireccion && !tieneLatitud && !tieneLongitud)
				|| (tieneDireccion && !tieneIdentificador && !tieneLatitud && !tieneLongitud)
				|| (!tieneIdentificador && !tieneDireccion && tieneLatitud && tieneLongitud);
	}
}
