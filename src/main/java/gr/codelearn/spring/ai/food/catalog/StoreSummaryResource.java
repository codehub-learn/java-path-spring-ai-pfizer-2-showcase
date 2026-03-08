package gr.codelearn.spring.ai.food.catalog;

import java.util.Set;

public record StoreSummaryResource(
		String storeId,
		String name,
		Set<Cuisine> cuisines,
		boolean openNow
) {
}