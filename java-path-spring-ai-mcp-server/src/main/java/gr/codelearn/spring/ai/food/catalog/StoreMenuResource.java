package gr.codelearn.spring.ai.food.catalog;

import java.util.List;
import java.util.Set;

public record StoreMenuResource(
		String storeId,
		String storeName,
		Set<Cuisine> cuisines,
		boolean openNow,
		List<MenuItemResource> items
) {
}