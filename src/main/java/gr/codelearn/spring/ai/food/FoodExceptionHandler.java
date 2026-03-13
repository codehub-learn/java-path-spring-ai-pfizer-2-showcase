package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.food.exception.CatalogQueryException;
import gr.codelearn.spring.ai.food.exception.FoodAssistantException;
import gr.codelearn.spring.ai.food.exception.StoreNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FoodExceptionHandler {
	@ExceptionHandler(StoreNotFoundException.class)
	public ProblemDetail handleStoreNotFound(StoreNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("Store not found");
		problem.setDetail(ex.getMessage());
		return problem;
	}

	@ExceptionHandler(CatalogQueryException.class)
	public ProblemDetail handleCatalogQuery(CatalogQueryException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Catalog query failed");
		problem.setDetail("The catalog request could not be completed.");
		return problem;
	}

	@ExceptionHandler(FoodAssistantException.class)
	public ProblemDetail handleFoodAssistant(FoodAssistantException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Food assistant failed");
		problem.setDetail("The assistant could not complete the request.");
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneric(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Unexpected error");
		problem.setDetail("An unexpected error occurred.");
		return problem;
	}
}