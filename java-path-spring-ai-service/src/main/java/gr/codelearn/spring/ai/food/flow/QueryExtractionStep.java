package gr.codelearn.spring.ai.food.flow;

import gr.codelearn.spring.ai.food.FoodQueryExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class QueryExtractionStep implements FoodFlowStep {
	private final FoodQueryExtractor foodQueryExtractor;

	@Override
	public void apply(FoodFlowContext context) {
		context.setExtraction(foodQueryExtractor.extract(context.getQuestion()));
		log.debug("QueryExtractionStep -> question='{}', extraction={}", context.getQuestion(), context.getExtraction());

	}
}