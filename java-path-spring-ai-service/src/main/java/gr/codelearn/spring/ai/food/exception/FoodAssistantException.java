package gr.codelearn.spring.ai.food.exception;

public class FoodAssistantException extends RuntimeException {
	public FoodAssistantException(String message, Throwable cause) {
		super(message, cause);
	}
}