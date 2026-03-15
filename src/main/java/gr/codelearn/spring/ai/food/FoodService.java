package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.Key;
import gr.codelearn.spring.ai.food.flow.FoodFlowOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FoodService {
	private final FoodFlowOrchestrator foodFlowOrchestrator;

	public Answer ask(String question, Key key) {
		return foodFlowOrchestrator.handle(question, key);
	}
}