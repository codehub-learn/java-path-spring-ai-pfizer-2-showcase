package gr.codelearn.spring.ai.food.catalog;

import java.util.Arrays;
import java.util.Locale;

public enum MenuItemCategory {
	BOWLS,
	BREAD,
	BREAKFAST,
	BURGERS,
	BURRITOS,
	COFFEE,
	CURRIES,
	DESSERTS,
	DRINKS,
	MAIN,
	NOODLES,
	PASTA,
	PIZZA,
	SALADS,
	SANDWICHES,
	SIDES,
	SOUPS,
	STARTERS,
	SUSHI,
	TACOS,
	WRAPS;

	public static MenuItemCategory fromUserValue(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Menu item category value must not be blank");
		}

		String normalized = value.trim()
								 .toUpperCase(Locale.ROOT)
								 .replace('-', '_')
								 .replace(' ', '_');

		return Arrays.stream(values())
					 .filter(category -> category.name().equals(normalized))
					 .findFirst()
					 .orElseThrow(() -> new IllegalArgumentException("Unknown menu item category: " + value));
	}
}