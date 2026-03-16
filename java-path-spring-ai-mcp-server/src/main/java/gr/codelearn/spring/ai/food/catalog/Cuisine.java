package gr.codelearn.spring.ai.food.catalog;

public enum Cuisine {
	AMERICAN,
	BREAKFAST,
	BURGERS,
	CHINESE,
	DESSERTS,
	GREEK,
	HEALTHY,
	INDIAN,
	ITALIAN,
	JAPANESE,
	MEDITERRANEAN,
	MEXICAN,
	PIZZA,
	SUSHI,
	VEGAN;

	public static Cuisine fromUserValue(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Cuisine value must not be blank");
		}

		String normalized = value.trim()
								 .toUpperCase(java.util.Locale.ROOT)
								 .replace('-', '_')
								 .replace(' ', '_');

		return java.util.Arrays.stream(values())
							   .filter(c -> c.name().equals(normalized))
							   .findFirst()
							   .orElseThrow(() -> new IllegalArgumentException("Unknown cuisine: " + value));
	}
}