package ies.belgrano.lotes.exception;

import ies.belgrano.lotes.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Todas las respuestas de error de la API usan {@link ApiErrorResponse}. Nunca incluyen trazas, rutas internas
 * ni el mensaje de una excepción inesperada: el detalle técnico queda en el log con el mismo correlacionId.
 */
@RestControllerAdvice
public class RestExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

	private static ResponseEntity<ApiErrorResponse> error(int status, String codigo, String mensaje,
			HttpServletRequest request, List<ApiErrorResponse.DetalleError> errores) {
		return ResponseEntity.status(status).body(new ApiErrorResponse(
				Instant.now(), status, codigo, mensaje, request.getRequestURI(), errores));
	}

	private static ResponseEntity<ApiErrorResponse> error(int status, String codigo, String mensaje,
			HttpServletRequest request) {
		return error(status, codigo, mensaje, request, List.of());
	}

	@ExceptionHandler(OperacionInvalidaException.class)
	public ResponseEntity<ApiErrorResponse> operacion(OperacionInvalidaException e, HttpServletRequest r) {
		return error(e.status, e.codigo, e.getMessage(), r);
	}

	@ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
	public ResponseEntity<ApiErrorResponse> fichaConcurrente(Exception e, HttpServletRequest r) {
		return error(409, "FICHA_MODIFICADA", "Otra persona modificó la ficha. Recargala antes de guardar.", r);
	}

	@ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> conflicto(Exception e, HttpServletRequest r) {
		return error(409, "CONFLICTO_DATOS", "Los datos entraron en conflicto con otro registro", r);
	}

	@ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
		org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
		org.springframework.web.bind.MissingServletRequestParameterException.class})
	public ResponseEntity<ApiErrorResponse> formato(Exception e, HttpServletRequest r) {
		return error(400, "DATOS_INVALIDOS", "Revisá el formato y los parámetros enviados", r);
	}

	@ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
	public ResponseEntity<ApiErrorResponse> validacionDeParametros(Exception e, HttpServletRequest r) {
		return error(400, "CONSULTA_INVALIDA", "Los parámetros de consulta no son válidos", r);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> rutaInexistente(Exception e, HttpServletRequest r) {
		return error(404, "RECURSO_NO_ENCONTRADO", "El recurso solicitado no existe", r);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> metodoNoPermitido(Exception e, HttpServletRequest r) {
		return error(405, "METODO_NO_PERMITIDO", "Esa operación no está disponible para este recurso", r);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> formatoNoSoportado(Exception e, HttpServletRequest r) {
		return error(415, "FORMATO_NO_SOPORTADO", "El formato de los datos enviados no es compatible", r);
	}

	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<ApiErrorResponse> formatoNoAceptable(Exception e, HttpServletRequest r) {
		return error(406, "FORMATO_NO_ACEPTABLE", "No se puede responder en el formato pedido", r);
	}

	/** La base de datos no responde: se avisa sin repetir la URL ni el usuario que trae el mensaje original. */
	@ExceptionHandler({org.springframework.dao.DataAccessResourceFailureException.class,
		org.springframework.transaction.CannotCreateTransactionException.class})
	public ResponseEntity<ApiErrorResponse> baseNoDisponible(Exception e, HttpServletRequest r) {
		LOGGER.error("La base de datos no está disponible: {} {}", r.getMethod(), r.getRequestURI(), e);
		return error(503, "BASE_DATOS_NO_DISPONIBLE",
			"El servicio no puede acceder a sus datos en este momento. Probá de nuevo en unos minutos.", r);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiErrorResponse> errorTecnico(RuntimeException e, HttpServletRequest r) {
		// Solo método y ruta: la consulta puede traer lo que la persona buscó.
		LOGGER.error("Error inesperado: {} {}", r.getMethod(), r.getRequestURI(), e);
		return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ERROR_INTERNO",
			"No se pudo completar la operación. Si se repite, informá el identificador de la solicitud.", r);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiErrorResponse> validacion(BindException exception, HttpServletRequest request) {
		List<ApiErrorResponse.DetalleError> detalles = new ArrayList<>();
		for (FieldError error : exception.getBindingResult().getFieldErrors()) {
			detalles.add(new ApiErrorResponse.DetalleError(error.getField(), error.getDefaultMessage()));
		}
		for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
			detalles.add(new ApiErrorResponse.DetalleError("criterio", error.getDefaultMessage()));
		}
		detalles.sort(Comparator.comparing(ApiErrorResponse.DetalleError::campo));
		return error(400, "CONSULTA_INVALIDA", "Los parámetros de consulta no son válidos", request, detalles);
	}
}
