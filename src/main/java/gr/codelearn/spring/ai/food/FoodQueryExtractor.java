package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.food.catalog.Cuisine;
import gr.codelearn.spring.ai.food.catalog.MenuItemCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class FoodQueryExtractor {
	private static final String EXTRACTION_PROMPT = """
													Extract structured information from the user's QuickBite question.
													
													Return null for unknown values.
													Do not invent values.
													If a cuisine is recognized, return the exact uppercase enum value.
													If a menu item category is recognized, return the exact uppercase enum value.
													If the question asks for a full menu, set menuRequest to true.
													If the question asks what the user can order, set availableOnly to true.
													If the question contains a support, policy, or procedure topic, capture it in supportTopic.
													
													User question:
													%s
													
													%s
													""";

	private static final Set<String> MENU_HINTS = Set.of("menu", "menus");
	private static final Set<String> AVAILABLE_HINTS = Set.of(
			"what can i order", "available", "availability", "can i order", "in stock", "serving", "serve", "serves"
															 );
	private static final Set<String> SUPPORT_HINTS = Set.of(
			"policy", "policies", "refund", "refunds", "cancel", "cancellation",
			"delivery", "support", "procedure", "procedures", "courier", "partner"
														   );

	@Qualifier("foodExtractionChatClient")
	private final ChatClient foodExtractionChatClient;

	public FoodQueryExtraction extract(String question) {
		FoodQueryExtraction llmExtraction = tryLlmExtraction(question);

		if (hasMeaningfulData(llmExtraction)) {
			log.debug("FoodQueryExtractor LLM extraction succeeded: {}", llmExtraction);
			return llmExtraction;
		}

		FoodQueryExtraction fallbackExtraction = fallbackExtract(question);
		log.debug("FoodQueryExtractor fallback extraction used: {}", fallbackExtraction);
		return fallbackExtraction;
	}

	private FoodQueryExtraction tryLlmExtraction(String question) {
		try {
			var converter = new BeanOutputConverter<>(FoodQueryExtraction.class);

			FoodQueryExtraction extraction = foodExtractionChatClient.prompt()
																	 .user(EXTRACTION_PROMPT.formatted(question, converter.getFormat()))
																	 .call()
																	 .entity(converter);

			return extraction != null ? extraction : FoodQueryExtraction.empty();
		} catch (Exception ex) {
			log.warn("FoodQueryExtractor LLM extraction failed for question='{}'. Falling back to heuristic extraction.", question, ex);
			return FoodQueryExtraction.empty();
		}
	}

	private FoodQueryExtraction fallbackExtract(String question) {
		String normalized = normalize(question);

		String cuisine = findCuisine(normalized).map(Enum::name).orElse(null);
		String category = findCategory(normalized).map(Enum::name).orElse(null);
		String itemName = null;
		String storeName = extractStoreName(question);
		String supportTopic = containsAny(normalized, SUPPORT_HINTS) ? question.trim() : null;
		Boolean menuRequest = containsAny(normalized, MENU_HINTS) ? Boolean.TRUE : null;
		Boolean availableOnly = containsAny(normalized, AVAILABLE_HINTS) ? Boolean.TRUE : null;

		return new FoodQueryExtraction(
				cuisine,
				category,
				itemName,
				storeName,
				supportTopic,
				menuRequest,
				availableOnly
		);
	}

	private boolean hasMeaningfulData(FoodQueryExtraction extraction) {
		return extraction != null
			   && (StringUtils.hasText(extraction.cuisine())
				   || StringUtils.hasText(extraction.category())
				   || StringUtils.hasText(extraction.itemName())
				   || StringUtils.hasText(extraction.storeName())
				   || StringUtils.hasText(extraction.supportTopic())
				   || extraction.menuRequest() != null
				   || extraction.availableOnly() != null);
	}

	private Optional<Cuisine> findCuisine(String normalizedQuestion) {
		return Arrays.stream(Cuisine.values())
					 .filter(cuisine -> matchesEnum(normalizedQuestion, cuisine.name()))
					 .findFirst();
	}

	private Optional<MenuItemCategory> findCategory(String normalizedQuestion) {
		return Arrays.stream(MenuItemCategory.values())
					 .filter(category -> matchesEnum(normalizedQuestion, category.name()))
					 .findFirst();
	}

	private boolean matchesEnum(String normalizedQuestion, String enumName) {
		String spaced = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
		String compact = spaced.replace(" ", "");
		return normalizedQuestion.contains(spaced) || normalizedQuestion.contains(compact);
	}

	private String extractStoreName(String question) {
		String normalized = normalize(question);

		if (normalized.contains("store-")) {
			int start = normalized.indexOf("store-");
			int end = normalized.indexOf(' ', start);
			if (end == -1) {
				end = normalized.length();
			}
			return question.substring(start, end).trim();
		}

		return null;
	}

	private boolean containsAny(String text, Set<String> hints) {
		return hints.stream().anyMatch(text::contains);
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
	}
}