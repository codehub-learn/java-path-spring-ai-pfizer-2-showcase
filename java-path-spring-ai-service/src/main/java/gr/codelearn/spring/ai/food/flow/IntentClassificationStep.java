package gr.codelearn.spring.ai.food.flow;

import gr.codelearn.spring.ai.food.FoodIntentRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class IntentClassificationStep implements FoodFlowStep {
	private final FoodIntentRouter foodIntentRouter;

	@Override
	public void apply(FoodFlowContext context) {
		context.setIntent(foodIntentRouter.classify(context.getQuestion()));
	}
}
