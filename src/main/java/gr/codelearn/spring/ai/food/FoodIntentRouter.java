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
			"what can i order",
			"what restaurants",
			"which restaurants",
			"which stores"
														   );

	private static final Set<String> SUPPORT_HINTS = Set.of(
			"service name",
			"quickbite",
			"policy", "policies",
			"support",
			"delivery",
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
			"process"
														   );

	public FoodIntent classify(String question) {
		if (!StringUtils.hasText(question)) {
			return FoodIntent.SUPPORT;
		}

		String normalized = normalize(question);

		boolean catalog = containsAny(normalized, CATALOG_HINTS);
		boolean support = containsAny(normalized, SUPPORT_HINTS);

		if (catalog && support) {
			return FoodIntent.BOTH;
		}
		if (catalog) {
			return FoodIntent.CATALOG;
		}
		return FoodIntent.SUPPORT;
	}

	private boolean containsAny(String text, Set<String> hints) {
		return hints.stream().anyMatch(text::contains);
	}

	private String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).trim();
	}
}