package ies.belgrano.lotes.exception;
public class OperacionInvalidaException extends RuntimeException {
    public final int status; public final String codigo;
    public OperacionInvalidaException(int status,String codigo,String mensaje) {super(mensaje);this.status=status;this.codigo=codigo;}
}
