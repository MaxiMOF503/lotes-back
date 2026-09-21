package ies.belgrano.lotes.exception;

public class LoteNoEncontradoException extends RuntimeException {

	public LoteNoEncontradoException() {
		super("No existe un lote demostrativo asociado al criterio indicado");
	}
}
