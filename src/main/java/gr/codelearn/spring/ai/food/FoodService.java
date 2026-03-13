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
import org.springframework.util.StringUtils;

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
												 
												 Focus topic: %s
												 Original user question: %s
												 """;

	private static final String CATALOG_PROMPT = """
												 The user is asking a catalog question about QuickBite stores or menus.
												 
												 Catalog routing hint: %s
												 
												 Extracted catalog filters:
												 - cuisine: %s
												 - category: %s
												 - itemName: %s
												 - storeName: %s
												 - menuRequest: %s
												 - availableOnly: %s
												 
												 Tool selection guidance:
												 - TITLE -> use find-stores-by-title
												 - CUISINE -> use find-stores-by-cuisine
												 - CATEGORY -> use find-stores-by-menu-item-category
												 - ITEM_NAME -> use find-stores-by-menu-item-name
												 - STORE_MENU -> use get-store-menu
												 - GENERIC -> choose the most relevant catalog tool
												 
												 Tool argument rules:
												 - When a tool expects an enum value, you must use the exact enum constant name expected by Java.
												 - Cuisine values must be uppercase enum names such as: %s
												 - Menu item category values must be uppercase enum names such as: %s
												 - Never use lowercase enum values like "italian"; use "ITALIAN" instead.
												 - If you are unsure about the exact enum value, prefer another tool or ask for clarification rather than guessing.
												 
												 Grounding rules:
												 - You must use the available tools to retrieve catalog data.
												 - Return only stores and menu items actually returned by the tools.
												 - Do not invent, assume, infer, complete, or suggest store names that were not returned.
												 - Do not use general world knowledge for stores or menus.
												 - If no matching stores are returned, say: "No matching stores were found in the QuickBite catalog."
												 - If no matching menu items are returned, say that clearly and do not guess.
												 
												 User question: %s
												 """;

	private static final Set<String> MENU_HINTS = Set.of("menu", "menus");
	private static final Set<String> TITLE_HINTS = Set.of("named", "called", "title");
	private static final Set<String> ITEM_NAME_HINTS = Set.of("nigiri", "maki", "roll", "rolls", "tiramisu", "brownie", "cheesecake",
															  "churros", "burrito", "burritos", "mochi", "pepperoni", "margherita");

	private final @Qualifier("foodSupportChatClient") ChatClient foodSupportChatClient;
	private final @Qualifier("foodCatalogChatClient") ChatClient foodCatalogChatClient;
	private final FoodIntentRouter foodIntentRouter;
	private final FoodQueryExtractor foodQueryExtractor;

	public Answer ask(String question, Key key) {
		Instant startedAt = Instant.now();

		try {
			// Step 1: classify the incoming question into SUPPORT, CATALOG, or BOTH.
			var intent = foodIntentRouter.classify(question);

			// Step 2: extract structured signals from the natural-language question.
			// This gives the next steps more precise inputs such as cuisine, category,
			// item name, store name, menu request, and support topic.
			var extraction = foodQueryExtractor.extract(question);

			log.debug("Food multi-step flow -> intent: {}, extraction: {}", intent, extraction);

			// Step 3: route execution to the correct branch.
			// - SUPPORT  -> support-only prompt and answer
			// - CATALOG  -> tool-grounded catalog lookup
			// - BOTH     -> execute both branches and merge the final answer
			return switch (intent) {
				case SUPPORT -> toAnswer(call(foodSupportChatClient, supportPrompt(question, extraction), key), elapsedMillis(startedAt));
				case CATALOG -> handleCatalog(question, key, startedAt, extraction);
				case BOTH -> askBoth(question, key, startedAt, extraction);
			};
		} catch (Exception e) {
			log.error("FoodService.ask failed for question='{}'.", question, e);
			return errorAnswer("Sorry, I could not process your request right now. Please try again.", elapsedMillis(startedAt),
							   Map.of("error", e.getClass().getSimpleName()));
		}
	}

	private Answer handleCatalog(String question, Key key, Instant startedAt, FoodQueryExtraction extraction) {
		// Step 4A: validate that the catalog branch has enough signal to search with confidence.
		// If the question is too vague, ask for clarification instead of guessing.
		if (!hasCatalogSignal(question, extraction)) {
			return clarificationAnswer(
					"Please tell me what you want to search for in the catalog: cuisine, category, menu item, or store name.",
					elapsedMillis(startedAt), extraction);
		}

		// Step 5A: execute the catalog branch using the catalog chat client and tools.
		return toAnswer(call(foodCatalogChatClient, catalogPrompt(question, extraction), key), elapsedMillis(startedAt));
	}

	private Answer askBoth(String question, Key key, Instant startedAt, FoodQueryExtraction extraction) {
		ChatResponse supportResponse = null;
		ChatResponse catalogResponse = null;
		Exception supportError = null;
		Exception catalogError = null;

		// Step 4B: decide whether the catalog half of a BOTH request has enough
		// information to run immediately or whether it should ask for clarification.
		boolean catalogSkippedForClarification = !hasCatalogSignal(question, extraction);

		try {
			// Step 5B.1: execute the support branch first.
			supportResponse = call(foodSupportChatClient, supportPrompt(question, extraction), key);
		} catch (Exception e) {
			supportError = e;
			log.warn("Support branch failed for question='{}'.", question, e);
		}

		if (!catalogSkippedForClarification) {
			try {
				// Step 5B.2: execute the catalog branch if enough catalog signal exists.
				catalogResponse = call(foodCatalogChatClient, catalogPrompt(question, extraction), key);
			} catch (Exception e) {
				catalogError = e;
				log.warn("Catalog branch failed for question='{}'.", question, e);
			}
		}

		long responseTimeMs = elapsedMillis(startedAt);

		// Step 6: merge branch results when both succeeded.
		if (supportResponse != null && catalogResponse != null) {
			return combineAnswers(supportResponse, catalogResponse, responseTimeMs);
		}

		// Step 7: return the successful branch and explain what happened to the other.
		if (supportResponse != null && catalogSkippedForClarification) {
			Answer answer = toAnswer(supportResponse, responseTimeMs);
			return new Answer(
					answer.answer() + "\n\nIf you also want catalog results, tell me a cuisine, category, menu item, or store name.",
					answer.promptTokens(), answer.completionTokens(), answer.totalTokens(), answer.responseTimeMs(),
					appendBranchMetadata(answer.modelMetadata(), "catalogStatus", "clarification_needed"));
		}

		if (supportResponse != null) {
			Answer answer = toAnswer(supportResponse, responseTimeMs);
			return new Answer(answer.answer() + "\n\nCatalog information could not be retrieved at the moment.", answer.promptTokens(),
							  answer.completionTokens(), answer.totalTokens(), answer.responseTimeMs(),
							  appendErrorMetadata(answer.modelMetadata(), "catalog", catalogError));
		}

		if (catalogResponse != null) {
			Answer answer = toAnswer(catalogResponse, responseTimeMs);
			return new Answer("Support information could not be retrieved at the moment.\n\n" + answer.answer(), answer.promptTokens(),
							  answer.completionTokens(), answer.totalTokens(), answer.responseTimeMs(),
							  appendErrorMetadata(answer.modelMetadata(), "support", supportError));
		}

		return errorAnswer("Sorry, I could not retrieve either support or catalog information right now. Please try again.",
						   responseTimeMs,
						   Map.of("supportError", errorName(supportError), "catalogError",
								  catalogSkippedForClarification ? "clarification_needed" : errorName(catalogError)));
	}

	private String supportPrompt(String question, FoodQueryExtraction extraction) {
		// Step 3A: narrow the support branch to the most relevant support topic.
		String topic = StringUtils.hasText(extraction.supportTopic()) ? extraction.supportTopic() : question;
		return SUPPORT_PROMPT.formatted(topic, question);
	}

	private String catalogPrompt(String question, FoodQueryExtraction extraction) {
		// Step 3B: convert the extracted signals into a catalog search mode
		// so the model is nudged toward the right tool.
		CatalogSearchMode mode = classifyCatalogSearchMode(question, extraction);

		return CATALOG_PROMPT.formatted(mode.name(), valueOrUnknown(extraction.cuisine()), valueOrUnknown(extraction.category()),
										valueOrUnknown(extraction.itemName()), valueOrUnknown(extraction.storeName()),
										valueOrUnknown(extraction.menuRequest()), valueOrUnknown(extraction.availableOnly()),
										enumValues(Cuisine.values()), enumValues(MenuItemCategory.values()), question);
	}

	private CatalogSearchMode classifyCatalogSearchMode(String question, FoodQueryExtraction extraction) {
		String q = normalize(question);

		// Step 3B.1: menu-specific requests go to STORE_MENU when enough store context exists.
		if (Boolean.TRUE.equals(extraction.menuRequest()) && StringUtils.hasText(extraction.storeName())) {
			return CatalogSearchMode.STORE_MENU;
		}

		if (containsAny(q, MENU_HINTS) && containsStoreIdLikeToken(q)) {
			return CatalogSearchMode.STORE_MENU;
		}

		if (containsStoreIdLikeToken(q)) {
			return CatalogSearchMode.STORE_MENU;
		}

		// Step 3B.2: cuisine requests are routed to the cuisine search tool.
		if (StringUtils.hasText(extraction.cuisine()) || containsEnumTerm(q, cuisineTerms())) {
			return CatalogSearchMode.CUISINE;
		}

		// Step 3B.3: category requests are routed to the category search tool.
		if (StringUtils.hasText(extraction.category()) || containsEnumTerm(q, categoryTerms())) {
			return CatalogSearchMode.CATEGORY;
		}

		// Step 3B.4: specific dish requests are routed to the item-name search tool.
		if (StringUtils.hasText(extraction.itemName()) || containsAny(q, ITEM_NAME_HINTS)) {
			return CatalogSearchMode.ITEM_NAME;
		}

		// Step 3B.5: store-name-like requests are routed to title search.
		if (StringUtils.hasText(extraction.storeName()) || containsAny(q, TITLE_HINTS) || q.contains("store") || q.contains("restaurant")) {
			return CatalogSearchMode.TITLE;
		}

		// Step 3B.6: fallback mode when the request is catalog-related but not strongly specific.
		return CatalogSearchMode.GENERIC;
	}

	private boolean hasCatalogSignal(String question, FoodQueryExtraction extraction) {
		String q = normalize(question);

		// Step 4 helper: detect whether we have enough catalog evidence to run a grounded lookup.
		return StringUtils.hasText(extraction.cuisine()) || StringUtils.hasText(extraction.category()) || StringUtils.hasText(
				extraction.itemName()) || StringUtils.hasText(extraction.storeName()) || Boolean.TRUE.equals(extraction.menuRequest()) ||
			   containsStoreIdLikeToken(q) || containsEnumTerm(q, cuisineTerms()) || containsEnumTerm(q, categoryTerms()) || containsAny(q,
																																		 ITEM_NAME_HINTS) ||
			   q.contains("store") || q.contains("restaurant") || q.contains("menu");
	}

	private boolean containsStoreIdLikeToken(String text) {
		return text.contains("store-");
	}

	private boolean containsEnumTerm(String text, Set<String> terms) {
		return terms.stream().anyMatch(text::contains);
	}

	private Set<String> cuisineTerms() {
		return Arrays.stream(Cuisine.values()).map(Enum::name).map(this::normalizeEnumValue).collect(Collectors.toUnmodifiableSet());
	}

	private Set<String> categoryTerms() {
		return Arrays.stream(MenuItemCategory.values()).map(Enum::name).map(this::normalizeEnumValue).collect(
				Collectors.toUnmodifiableSet());
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

	private String enumValues(Enum<?>[] values) {
		return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
	}

	private String valueOrUnknown(Object value) {
		return value == null ? "unknown" : String.valueOf(value);
	}

	private ChatResponse call(ChatClient chatClient, String prompt, Key key) {
		// Execution helper used by each branch to call the model with conversation memory.
		ChatResponse chatResponse = chatClient.prompt().user(prompt).advisors(
				advisor -> advisor.param(ChatMemory.CONVERSATION_ID, key.toString())).call().chatResponse();

		return Objects.requireNonNull(chatResponse);
	}

	private Answer toAnswer(ChatResponse chatResponse, long responseTimeMs) {
		String text = Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();

		return new Answer(text, chatResponse.getMetadata().getUsage().getPromptTokens(),
						  chatResponse.getMetadata().getUsage().getCompletionTokens(),
						  chatResponse.getMetadata().getUsage().getTotalTokens(), responseTimeMs,
						  chatResponse.getResult().getOutput().getMetadata());
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

		Integer promptTokens = sum(supportResponse.getMetadata().getUsage().getPromptTokens(),
								   catalogResponse.getMetadata().getUsage().getPromptTokens());
		Integer completionTokens = sum(supportResponse.getMetadata().getUsage().getCompletionTokens(),
									   catalogResponse.getMetadata().getUsage().getCompletionTokens());
		Integer totalTokens = sum(supportResponse.getMetadata().getUsage().getTotalTokens(),
								  catalogResponse.getMetadata().getUsage().getTotalTokens());

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("support", supportResponse.getResult().getOutput().getMetadata());
		metadata.put("catalog", catalogResponse.getResult().getOutput().getMetadata());

		return new Answer(combinedText, promptTokens, completionTokens, totalTokens, responseTimeMs, metadata);
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

	private Answer clarificationAnswer(String message, long responseTimeMs, FoodQueryExtraction extraction) {
		return new Answer(message, 0, 0, 0, responseTimeMs,
						  Map.of("flow", "multi-step", "status", "clarification_needed", "extraction", extraction));
	}

	private Answer errorAnswer(String message, long responseTimeMs, Map<String, Object> metadata) {
		return new Answer(message, 0, 0, 0, responseTimeMs, metadata);
	}

	private Map<String, Object> appendErrorMetadata(Map<String, Object> metadata, String branch, Exception error) {
		Map<String, Object> enriched = new LinkedHashMap<>();
		if (metadata != null) {
			enriched.putAll(metadata);
		}
		enriched.put(branch + "Error", errorName(error));
		return enriched;
	}

	private Map<String, Object> appendBranchMetadata(Map<String, Object> metadata, String key, String value) {
		Map<String, Object> enriched = new LinkedHashMap<>();
		if (metadata != null) {
			enriched.putAll(metadata);
		}
		enriched.put(key, value);
		return enriched;
	}

	private String errorName(Exception error) {
		return error == null ? null : error.getClass().getSimpleName();
	}

	private enum CatalogSearchMode {
		TITLE, CUISINE, CATEGORY, ITEM_NAME, STORE_MENU, GENERIC
	}
}