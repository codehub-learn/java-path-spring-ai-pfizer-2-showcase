package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.Key;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class FoodService {
	@Qualifier("foodSupportChatClient")
	private final ChatClient foodSupportChatClient;
	@Qualifier("foodCatalogChatClient")
	private final ChatClient foodCatalogChatClient;
	private final FoodIntentRouter foodIntentRouter;
	private static final String SUPPORT_PROMPT = """
												 Answer the user's question using only the QuickBite support knowledge base context.
												 If the answer is not present in the context, say that you do not know.
												 
												 User question: %s
												 """;

	private static final String CATALOG_PROMPT = """
												 Answer the user's question using the available QuickBite catalog tools.
												 If there are no matching stores or menus, say that clearly.
												 
												 User question: %s
												 """;

	public Answer ask(String question, final Key key) {
		var start = Instant.now();
		FoodIntent intent = foodIntentRouter.classify(question);

		return switch (intent) {
			case SUPPORT -> toAnswer(call(foodSupportChatClient, SUPPORT_PROMPT.formatted(question), key), start);
			case CATALOG -> toAnswer(call(foodCatalogChatClient, CATALOG_PROMPT.formatted(question), key), start);
			case BOTH -> combineAnswers(call(foodSupportChatClient, SUPPORT_PROMPT.formatted(question), key),
										call(foodCatalogChatClient, CATALOG_PROMPT.formatted(question), key), start);
		};

	}

	private ChatResponse call(final ChatClient chatClient, final String prompt, final Key key) {
		ChatResponse chatResponse = chatClient.prompt()
											  .user(prompt)
											  .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, key.toString()))
											  .call()
											  .chatResponse();

		return Objects.requireNonNull(chatResponse);
	}

	private Answer toAnswer(final ChatResponse chatResponse, final Instant start) {
		String text = Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();

		return new Answer(
				text,
				chatResponse.getMetadata().getUsage().getPromptTokens(),
				chatResponse.getMetadata().getUsage().getCompletionTokens(),
				chatResponse.getMetadata().getUsage().getTotalTokens(),
				Duration.between(start, Instant.now()).toMillis(),
				chatResponse.getResult().getOutput().getMetadata()
		);
	}

	private Answer combineAnswers(final ChatResponse supportResponse, final ChatResponse catalogResponse, final Instant start) {
		String supportText = extractText(supportResponse);
		String catalogText = extractText(catalogResponse);

		String combinedText = """
							  Support information:
							  %s
							  
							  Catalog information:
							  %s
							  """.formatted(supportText, catalogText);

		Integer promptTokens = sum(supportResponse.getMetadata().getUsage().getPromptTokens(),
								   catalogResponse.getMetadata().getUsage().getPromptTokens());
		Integer completionTokens = sum(supportResponse.getMetadata().getUsage().getCompletionTokens(),
									   catalogResponse.getMetadata().getUsage().getCompletionTokens());
		Integer totalTokens = sum(supportResponse.getMetadata().getUsage().getTotalTokens(),
								  catalogResponse.getMetadata().getUsage().getTotalTokens());

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("support", supportResponse.getResult().getOutput().getMetadata());
		metadata.put("catalog", catalogResponse.getResult().getOutput().getMetadata());

		return new Answer(
				combinedText,
				promptTokens,
				completionTokens,
				totalTokens,
				Duration.between(start, Instant.now()).toMillis(),
				metadata
		);
	}

	private String extractText(ChatResponse chatResponse) {
		return Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();
	}

	private Integer sum(Integer left, Integer right) {
		int safeLeft = left == null ? 0 : left;
		int safeRight = right == null ? 0 : right;
		return safeLeft + safeRight;
	}
}