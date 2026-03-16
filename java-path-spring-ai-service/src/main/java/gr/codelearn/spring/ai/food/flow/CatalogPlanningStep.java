package gr.codelearn.spring.ai.food.flow;

import gr.codelearn.spring.ai.food.catalog.Cuisine;
import gr.codelearn.spring.ai.food.catalog.MenuItemCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class CatalogPlanningStep implements FoodFlowStep {
	private static final Set<String> MENU_HINTS = Set.of("menu", "menus");
	private static final Set<String> TITLE_HINTS = Set.of("named", "called", "title");
	private static final Set<String> ITEM_NAME_HINTS = Set.of("nigiri", "maki", "roll", "rolls", "tiramisu", "brownie", "cheesecake",
															  "churros", "burrito", "burritos", "mochi", "pepperoni", "margherita");

	@Override
	public void apply(FoodFlowContext context) {
		context.setCatalogSearchMode(classifyCatalogSearchMode(context));
		context.setCatalogClarificationNeeded(!hasCatalogSignal(context));
	}

	private CatalogSearchMode classifyCatalogSearchMode(FoodFlowContext context) {
		String q = normalize(context.getQuestion());
		var extraction = context.getExtraction();

		if (Boolean.TRUE.equals(extraction.menuRequest()) && StringUtils.hasText(extraction.storeName())) {
			return CatalogSearchMode.STORE_MENU;
		}

		if (containsAny(q, MENU_HINTS) && containsStoreIdLikeToken(q)) {
			return CatalogSearchMode.STORE_MENU;
		}

		if (containsStoreIdLikeToken(q)) {
			return CatalogSearchMode.STORE_MENU;
		}

		if (StringUtils.hasText(extraction.cuisine()) || containsEnumTerm(q, cuisineTerms())) {
			return CatalogSearchMode.CUISINE;
		}

		if (StringUtils.hasText(extraction.category()) || containsEnumTerm(q, categoryTerms())) {
			return CatalogSearchMode.CATEGORY;
		}

		if (StringUtils.hasText(extraction.itemName()) || containsAny(q, ITEM_NAME_HINTS)) {
			return CatalogSearchMode.ITEM_NAME;
		}

		if (StringUtils.hasText(extraction.storeName()) || containsAny(q, TITLE_HINTS) || q.contains("store") || q.contains("restaurant")) {
			return CatalogSearchMode.TITLE;
		}

		return CatalogSearchMode.GENERIC;
	}

	private boolean hasCatalogSignal(FoodFlowContext context) {
		String q = normalize(context.getQuestion());
		var extraction = context.getExtraction();

		return StringUtils.hasText(extraction.cuisine())
			   || StringUtils.hasText(extraction.category())
			   || StringUtils.hasText(extraction.itemName())
			   || StringUtils.hasText(extraction.storeName())
			   || Boolean.TRUE.equals(extraction.menuRequest())
			   || containsStoreIdLikeToken(q)
			   || containsEnumTerm(q, cuisineTerms())
			   || containsEnumTerm(q, categoryTerms())
			   || containsAny(q, ITEM_NAME_HINTS)
			   || q.contains("store")
			   || q.contains("restaurant")
			   || q.contains("menu");
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
}