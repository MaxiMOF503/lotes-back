package ies.belgrano.lotes.exception;

import ies.belgrano.lotes.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<ApiErrorResponse> operacion(OperacionInvalidaException e,HttpServletRequest r) {
        return ResponseEntity.status(e.status).body(new ApiErrorResponse(Instant.now(),e.status,e.codigo,e.getMessage(),r.getRequestURI(),List.of()));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> conflicto(Exception e,HttpServletRequest r) {
        return ResponseEntity.status(409).body(new ApiErrorResponse(Instant.now(),409,"CONFLICTO_DATOS","Identificador duplicado o datos incompatibles",r.getRequestURI(),List.of()));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> formato(Exception e,HttpServletRequest r) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(Instant.now(),400,"DATOS_INVALIDOS","Revisá el formato y los parámetros enviados",r.getRequestURI(),List.of()));
    }
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiErrorResponse> handleErrorTecnico(
			RuntimeException exception, HttpServletRequest request) {
		LOGGER.error("Error inesperado al consultar la ficha pública", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
				Instant.now(), 500, "ERROR_INTERNO",
				"No se pudo completar la consulta", request.getRequestURI(), List.of()));
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiErrorResponse> handleBindException(
			BindException exception,
			HttpServletRequest request) {
		List<ApiErrorResponse.DetalleError> detalles = new ArrayList<>();

		for (FieldError error : exception.getBindingResult().getFieldErrors()) {
			detalles.add(new ApiErrorResponse.DetalleError(error.getField(), error.getDefaultMessage()));
		}
		for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
			detalles.add(new ApiErrorResponse.DetalleError("criterio", error.getDefaultMessage()));
		}
		detalles.sort(Comparator.comparing(ApiErrorResponse.DetalleError::campo));

		return ResponseEntity.badRequest().body(new ApiErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				"CONSULTA_INVALIDA",
				"Los parámetros de consulta no son válidos",
				request.getRequestURI(),
				detalles));
	}

	@ExceptionHandler(LoteNoEncontradoException.class)
	public ResponseEntity<ApiErrorResponse> handleLoteNoEncontrado(
			LoteNoEncontradoException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
				Instant.now(),
				HttpStatus.NOT_FOUND.value(),
				"LOTE_NO_ENCONTRADO",
				exception.getMessage(),
				request.getRequestURI(),
				List.of()));
	}
}
