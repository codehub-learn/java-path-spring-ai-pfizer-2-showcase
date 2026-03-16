package gr.codelearn.spring.ai.food.flow;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupportPromptFactory {
	private static final String SUPPORT_PROMPT = """
												 Answer the user's question using only the QuickBite support knowledge base context.
												 If the answer is not present in the context, say that you do not know.
												 
												 Focus topic: %s
												 Original user question: %s
												 """;

	public String create(FoodFlowContext context) {
		String topic = StringUtils.hasText(context.getExtraction().supportTopic())
					   ? context.getExtraction().supportTopic()
					   : context.getQuestion();

		return SUPPORT_PROMPT.formatted(topic, context.getQuestion());
	}
}