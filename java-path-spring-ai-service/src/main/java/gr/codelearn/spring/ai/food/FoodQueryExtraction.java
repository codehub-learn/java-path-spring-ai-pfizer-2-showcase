package gr.codelearn.spring.ai.food;

public record FoodQueryExtraction(
		String cuisine,
		String category,
		String itemName,
		String storeName,
		String supportTopic,
		Boolean menuRequest,
		Boolean availableOnly
) {
	public static FoodQueryExtraction empty() {
		return new FoodQueryExtraction(null, null, null, null, null, null, null);
	}
}
