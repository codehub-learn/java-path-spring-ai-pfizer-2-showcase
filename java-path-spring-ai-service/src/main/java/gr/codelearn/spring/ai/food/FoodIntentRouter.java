package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.food.catalog.Cuisine;
import gr.codelearn.spring.ai.food.catalog.MenuItemCategory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FoodIntentRouter {
	private static final Set<String> CATALOG_DISCOVERY_HINTS = Set.of(
			"store", "stores",
			"restaurant", "restaurants",
			"menu", "menus",
			"cuisine", "cuisines",
			"find", "show", "list",
			"serving", "serve", "serves",
			"offer", "offers",
			"have", "has",
			"what can i order",
			"where can i order",
			"which store",
			"which stores",
			"which restaurant",
			"which restaurants"
																	 );

	private static final Set<String> SUPPORT_HINTS = Set.of(
			"service name",
			"policy", "policies",
			"support",
			"delivery policy",
			"delivery partner",
			"partner", "partners",
			"courier", "couriers",
			"refund", "refunds",
			"cancel", "cancellation",
			"escalation", "escalate",
			"runbook",
			"knowledge base",
			"how does",
			"what happens",
			"failed delivery",
			"customer unavailable",
			"operational",
			"operations",
			"procedure", "procedures"
														   );

	private final Set<String> catalogTerms = buildCatalogTerms();

	public FoodIntent classify(String question) {
		if (!StringUtils.hasText(question)) {
			return FoodIntent.SUPPORT;
		}

		String normalized = normalize(question);

		boolean catalog = containsAny(normalized, CATALOG_DISCOVERY_HINTS)
						  || containsAny(normalized, catalogTerms)
						  || looksLikeCatalogDiscovery(normalized);

		boolean support = containsAny(normalized, SUPPORT_HINTS);

		if (catalog && support) {
			return FoodIntent.BOTH;
		}
		if (catalog) {
			return FoodIntent.CATALOG;
		}
		return FoodIntent.SUPPORT;
	}

	private boolean looksLikeCatalogDiscovery(String text) {
		boolean asksForCatalogEntities = text.contains("store")
										 || text.contains("stores")
										 || text.contains("restaurant")
										 || text.contains("restaurants")
										 || text.contains("menu");

		boolean asksForMatching = text.contains("serving")
								  || text.contains("serve")
								  || text.contains("serves")
								  || text.contains("have")
								  || text.contains("has")
								  || text.contains("offer")
								  || text.contains("offers");

		boolean mentionsCatalogTerm = catalogTerms.stream().anyMatch(text::contains);

		return asksForCatalogEntities || (asksForMatching && mentionsCatalogTerm);
	}

	private boolean containsAny(String text, Set<String> hints) {
		return hints.stream().anyMatch(text::contains);
	}

	private Set<String> buildCatalogTerms() {
		Set<String> cuisineTerms = Arrays.stream(Cuisine.values())
										 .flatMap(this::enumTokens)
										 .collect(Collectors.toSet());

		Set<String> categoryTerms = Arrays.stream(MenuItemCategory.values())
										  .flatMap(this::enumTokens)
										  .collect(Collectors.toSet());

		Set<String> extraFoodTerms = Set.of(
				"nigiri", "roll", "rolls", "maki",
				"tiramisu", "brownie", "cheesecake",
				"burrito", "burritos",
				"fries", "wings", "wrap", "wraps",
				"coffee", "drink", "drinks"
										   );

		return Stream.of(cuisineTerms.stream(), categoryTerms.stream(), extraFoodTerms.stream())
					 .flatMap(stream -> stream)
					 .collect(Collectors.toUnmodifiableSet());
	}

	private Stream<String> enumTokens(Enum<?> value) {
		String normalized = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
		String compact = normalized.replace(" ", "");

		return Stream.of(normalized, compact)
					 .flatMap(term -> Arrays.stream(term.split("\\s+")))
					 .filter(StringUtils::hasText)
					 .distinct();
	}

	private String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).trim();
	}
}