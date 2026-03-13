package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.Key;
import gr.codelearn.spring.ai.food.catalog.Cuisine;
import gr.codelearn.spring.ai.food.catalog.MenuItemCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class FoodService {
	private static final String SUPPORT_PROMPT = """
												 Answer the user's question using only the QuickBite support knowledge base context.
												 If the answer is not present in the context, say that you do not know.
												 
												 User question: %s
												 """;

	private static final String CATALOG_PROMPT = """
												 The user is asking a catalog question about QuickBite stores or menus.
												 
												 Catalog routing hint: %s
												 
												 Tool selection guidance:
												 - TITLE -> use find-stores-by-title
												 - CUISINE -> use find-stores-by-cuisine
												 - CATEGORY -> use find-stores-by-menu-item-category
												 - ITEM_NAME -> use find-stores-by-menu-item-name
												 - STORE_MENU -> use get-store-menu
												 - GENERIC -> choose the most relevant catalog tool
												 
												 User question: %s
												 """;

	private static final Set<String> MENU_HINTS = Set.of("menu", "menus");
	private static final Set<String> TITLE_HINTS = Set.of("named", "called", "title");
	private static final Set<String> ITEM_NAME_HINTS = Set.of("nigiri", "maki", "roll", "rolls", "tiramisu", "brownie", "cheesecake",
															  "churros", "burrito", "burritos", "mochi", "pepperoni", "margherita");

	private final @Qualifier("foodSupportChatClient") ChatClient foodSupportChatClient;
	private final @Qualifier("foodCatalogChatClient") ChatClient foodCatalogChatClient;
	private final FoodIntentRouter foodIntentRouter;

	public Answer ask(String question, Key key) {
		Instant startedAt = Instant.now();

		try {
			FoodIntent intent = foodIntentRouter.classify(question);

			return switch (intent) {
				case SUPPORT -> toAnswer(
						call(foodSupportChatClient, SUPPORT_PROMPT.formatted(question), key),
						elapsedMillis(startedAt)
										);
				case CATALOG -> toAnswer(
						call(foodCatalogChatClient, catalogPrompt(question), key),
						elapsedMillis(startedAt)
										);
				case BOTH -> askBoth(question, key, startedAt);
			};
		} catch (Exception e) {
			log.error("FoodService.ask failed for question='{}'.", question, e);
			return errorAnswer(
					"Sorry, I could not process your request right now. Please try again.",
					elapsedMillis(startedAt),
					Map.of("error", e.getClass().getSimpleName())
							  );
		}
	}

	private Answer askBoth(String question, Key key, Instant startedAt) {
		ChatResponse supportResponse = null;
		ChatResponse catalogResponse = null;
		Exception supportError = null;
		Exception catalogError = null;

		try {
			supportResponse = call(foodSupportChatClient, SUPPORT_PROMPT.formatted(question), key);
		} catch (Exception e) {
			supportError = e;
			log.warn("Support branch failed for question='{}'.", question, e);
		}

		try {
			catalogResponse = call(foodCatalogChatClient, catalogPrompt(question), key);
		} catch (Exception e) {
			catalogError = e;
			log.warn("Catalog branch failed for question='{}'.", question, e);
		}

		long responseTimeMs = elapsedMillis(startedAt);

		if (supportResponse != null && catalogResponse != null) {
			return combineAnswers(supportResponse, catalogResponse, responseTimeMs);
		}

		if (supportResponse != null) {
			Answer answer = toAnswer(supportResponse, responseTimeMs);
			return new Answer(
					answer.answer() + "\n\nCatalog information could not be retrieved at the moment.",
					answer.promptTokens(),
					answer.completionTokens(),
					answer.totalTokens(),
					answer.responseTimeMs(),
					appendErrorMetadata(answer.modelMetadata(), "catalog", catalogError)
			);
		}

		if (catalogResponse != null) {
			Answer answer = toAnswer(catalogResponse, responseTimeMs);
			return new Answer(
					"Support information could not be retrieved at the moment.\n\n" + answer.answer(),
					answer.promptTokens(),
					answer.completionTokens(),
					answer.totalTokens(),
					answer.responseTimeMs(),
					appendErrorMetadata(answer.modelMetadata(), "support", supportError)
			);
		}

		return errorAnswer(
				"Sorry, I could not retrieve either support or catalog information right now. Please try again.",
				responseTimeMs,
				Map.of(
						"supportError", errorName(supportError),
						"catalogError", errorName(catalogError)
					  )
						  );
	}

	private String catalogPrompt(String question) {
		CatalogSearchMode mode = classifyCatalogSearchMode(question);
		return CATALOG_PROMPT.formatted(mode.name(), question);
	}

	private CatalogSearchMode classifyCatalogSearchMode(String question) {
		String q = normalize(question);

		if (containsAny(q, MENU_HINTS) && containsStoreIdLikeToken(q)) {
			return CatalogSearchMode.STORE_MENU;
		}

		if (containsStoreIdLikeToken(q)) {
			return CatalogSearchMode.STORE_MENU;
		}

		if (containsEnumTerm(q, cuisineTerms())) {
			return CatalogSearchMode.CUISINE;
		}

		if (containsEnumTerm(q, categoryTerms())) {
			return CatalogSearchMode.CATEGORY;
		}

		if (containsAny(q, ITEM_NAME_HINTS)) {
			return CatalogSearchMode.ITEM_NAME;
		}

		if (containsAny(q, TITLE_HINTS) || q.contains("store") || q.contains("restaurant")) {
			return CatalogSearchMode.TITLE;
		}

		return CatalogSearchMode.GENERIC;
	}

	private boolean containsStoreIdLikeToken(String text) {
		return text.contains("store-");
	}

	private boolean containsEnumTerm(String text, Set<String> terms) {
		return terms.stream().anyMatch(text::contains);
	}

	private Set<String> cuisineTerms() {
		return Arrays.stream(Cuisine.values())
					 .map(Enum::name)
					 .map(this::normalizeEnumValue)
					 .collect(Collectors.toUnmodifiableSet());
	}

	private Set<String> categoryTerms() {
		return Arrays.stream(MenuItemCategory.values())
					 .map(Enum::name)
					 .map(this::normalizeEnumValue)
					 .collect(Collectors.toUnmodifiableSet());
	}

	private String normalizeEnumValue(String value) {
		return value.toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	private boolean containsAny(String text, Set<String> hints) {
		return hints.stream().anyMatch(text::contains);
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
	}

	private ChatResponse call(ChatClient chatClient, String prompt, Key key) {
		ChatResponse chatResponse = chatClient.prompt()
											  .user(prompt)
											  .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, key.toString()))
											  .call()
											  .chatResponse();

		return Objects.requireNonNull(chatResponse);
	}

	private Answer toAnswer(ChatResponse chatResponse, long responseTimeMs) {
		String text = Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();

		return new Answer(
				text,
				chatResponse.getMetadata().getUsage().getPromptTokens(),
				chatResponse.getMetadata().getUsage().getCompletionTokens(),
				chatResponse.getMetadata().getUsage().getTotalTokens(),
				responseTimeMs,
				chatResponse.getResult().getOutput().getMetadata()
		);
	}

	private Answer combineAnswers(ChatResponse supportResponse, ChatResponse catalogResponse, long responseTimeMs) {
		String supportText = extractText(supportResponse);
		String catalogText = extractText(catalogResponse);

		String combinedText = """
							  Support information:
							  %s
							  
							  Catalog information:
							  %s
							  """.formatted(supportText, catalogText);

		Integer promptTokens = sum(
				supportResponse.getMetadata().getUsage().getPromptTokens(),
				catalogResponse.getMetadata().getUsage().getPromptTokens()
								  );
		Integer completionTokens = sum(
				supportResponse.getMetadata().getUsage().getCompletionTokens(),
				catalogResponse.getMetadata().getUsage().getCompletionTokens()
									  );
		Integer totalTokens = sum(
				supportResponse.getMetadata().getUsage().getTotalTokens(),
				catalogResponse.getMetadata().getUsage().getTotalTokens()
								 );

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("support", supportResponse.getResult().getOutput().getMetadata());
		metadata.put("catalog", catalogResponse.getResult().getOutput().getMetadata());

		return new Answer(
				combinedText,
				promptTokens,
				completionTokens,
				totalTokens,
				responseTimeMs,
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

	private long elapsedMillis(Instant startedAt) {
		return Duration.between(startedAt, Instant.now()).toMillis();
	}

	private Answer errorAnswer(String message, long responseTimeMs, Map<String, Object> metadata) {
		return new Answer(
				message,
				0,
				0,
				0,
				responseTimeMs,
				metadata
		);
	}

	private Map<String, Object> appendErrorMetadata(Map<String, Object> metadata, String branch, Exception error) {
		Map<String, Object> enriched = new LinkedHashMap<>();
		if (metadata != null) {
			enriched.putAll(metadata);
		}
		enriched.put(branch + "Error", errorName(error));
		return enriched;
	}

	private String errorName(Exception error) {
		return error == null ? null : error.getClass().getSimpleName();
	}

	private enum CatalogSearchMode {
		TITLE,
		CUISINE,
		CATEGORY,
		ITEM_NAME,
		STORE_MENU,
		GENERIC
	}
}