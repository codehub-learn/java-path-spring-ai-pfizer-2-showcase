package gr.codelearn.spring.ai.food.flow;

import gr.codelearn.spring.ai.Key;
import gr.codelearn.spring.ai.food.FoodIntent;
import gr.codelearn.spring.ai.food.FoodQueryExtraction;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.model.ChatResponse;

import java.time.Instant;

@Getter
@Setter
public class FoodFlowContext {
	private final String question;
	private final Key key;
	private final Instant startedAt;

	private FoodIntent intent;
	private FoodQueryExtraction extraction = FoodQueryExtraction.empty();
	private CatalogSearchMode catalogSearchMode = CatalogSearchMode.GENERIC;
	private boolean catalogClarificationNeeded;

	private ChatResponse supportResponse;
	private ChatResponse catalogResponse;

	private Exception supportError;
	private Exception catalogError;

	public FoodFlowContext(String question, Key key) {
		this.question = question;
		this.key = key;
		this.startedAt = Instant.now();
	}
}
