package gr.codelearn.spring.ai.food;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FoodQueryExtractor {
	private static final String EXTRACTION_PROMPT = """
													Extract structured information from the user's QuickBite question.
													
													Return null for unknown values.
													Do not invent values.
													If a cuisine or category is recognized, return the exact enum-style uppercase value.
													If the question asks for a full menu, set menuRequest to true.
													If the question asks what the user can order, set availableOnly to true.
													If the question contains a support/policy/procedure topic, capture it in supportTopic.
													
													User question:
													%s
													
													%s
													""";

	@Qualifier("foodSupportChatClient")
	private final ChatClient chatClient;

	public FoodQueryExtraction extract(String question) {
		try {
			BeanOutputConverter<FoodQueryExtraction> converter = new BeanOutputConverter<>(FoodQueryExtraction.class);

			return chatClient.prompt()
							 .user(EXTRACTION_PROMPT.formatted(question, converter.getFormat()))
							 .call()
							 .entity(converter);
		} catch (Exception ex) {
			return FoodQueryExtraction.empty();
		}
	}
}