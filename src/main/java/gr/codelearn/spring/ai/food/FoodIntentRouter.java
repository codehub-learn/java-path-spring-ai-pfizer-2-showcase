package gr.codelearn.spring.ai.food;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Component
public class FoodIntentRouter {
	private static final Set<String> CATALOG_HINTS = Set.of(
			"store", "stores",
			"restaurant", "restaurants",
			"menu", "menus",
			"cuisine", "cuisines",
			"pizza", "burger", "burgers", "sushi", "taco", "tacos",
			"pasta", "salad", "dessert", "desserts",
			"find store", "find stores",
			"show store", "show stores",
			"list store", "list stores",
			"offer", "offers",
			"have", "has",
			"find", "show", "list",
			"where can i order",
			"what can i order",
			"which store",
			"which stores",
			"which restaurant",
			"which restaurants",
			"coffee", "drink", "drinks",
			"nigiri", "roll", "rolls", "maki",
			"churros", "tiramisu", "brownie", "cheesecake",
			"burrito", "burritos",
			"fries", "wings", "wrap", "wraps",
			"italian", "japanese", "mexican", "american", "healthy", "vegan");

	private static final Set<String> SUPPORT_HINTS = Set.of(
			"service name", "service", "guide",
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
			"kb",
			"knowledge base",
			"how does",
			"what happens",
			"failed delivery",
			"customer unavailable",
			"operational",
			"operations",
			"procedure", "procedures",
			"process", "order", "ordering");

	public FoodIntent classify(String question) {
		if (!StringUtils.hasText(question)) {
			return FoodIntent.SUPPORT;
		}

		String normalized = normalize(question);

		boolean catalog = containsAny(normalized, CATALOG_HINTS) || looksLikeCatalogDiscovery(normalized);
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
		boolean asksForStores = text.contains("store") || text.contains("stores")
								|| text.contains("restaurant") || text.contains("restaurants")
								|| text.contains("menu");

		boolean asksForFoodMatching = text.contains("serving")
									  || text.contains("serve")
									  || text.contains("have")
									  || text.contains("has")
									  || text.contains("offer")
									  || text.contains("offers");

		boolean mentionsFoodishTerm = text.contains("sushi")
									  || text.contains("pizza")
									  || text.contains("burger")
									  || text.contains("taco")
									  || text.contains("pasta")
									  || text.contains("dessert")
									  || text.contains("drink")
									  || text.contains("tiramisu")
									  || text.contains("nigiri")
									  || text.contains("burrito");

		return asksForStores || (asksForFoodMatching && mentionsFoodishTerm);
	}

	private boolean containsAny(String text, Set<String> hints) {
		return hints.stream().anyMatch(text::contains);
	}

	private String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).trim();
	}
}