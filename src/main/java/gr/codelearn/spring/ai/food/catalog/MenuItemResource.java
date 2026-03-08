package gr.codelearn.spring.ai.food.catalog;

import java.math.BigDecimal;

public record MenuItemResource(
		String name,
		String description,
		BigDecimal price,
		MenuItemCategory category,
		boolean available
) {
}