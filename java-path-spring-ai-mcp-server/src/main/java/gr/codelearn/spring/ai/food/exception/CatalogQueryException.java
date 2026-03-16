package gr.codelearn.spring.ai.food.exception;

public class CatalogQueryException extends RuntimeException {
	public CatalogQueryException(String message, Throwable cause) {
		super(message, cause);
	}
}