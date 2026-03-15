package gr.codelearn.spring.ai.food.flow;

import gr.codelearn.spring.ai.food.catalog.Cuisine;
import gr.codelearn.spring.ai.food.catalog.MenuItemCategory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class CatalogPromptFactory {
	private static final String CATALOG_PROMPT = """
												 The user is asking a catalog question about QuickBite stores or menus.
												 
												 Catalog routing hint: %s
												 
												 Extracted catalog filters:
												 - cuisine: %s
												 - category: %s
												 - itemName: %s
												 - storeName: %s
												 - menuRequest: %s
												 - availableOnly: %s
												 
												 Tool selection guidance:
												 - TITLE -> use find-stores-by-title
												 - CUISINE -> use find-stores-by-cuisine
												 - CATEGORY -> use find-stores-by-menu-item-category
												 - ITEM_NAME -> use find-stores-by-menu-item-name
												 - STORE_MENU -> use get-store-menu
												 - GENERIC -> choose the most relevant catalog tool
												 
												 Tool argument rules:
												 - When a tool expects an enum value, you must use the exact enum constant name expected by Java.
												 - Cuisine values must be uppercase enum names such as: %s
												 - Menu item category values must be uppercase enum names such as: %s
												 - Never use lowercase enum values like "italian"; use "ITALIAN" instead.
												 - If you are unsure about the exact enum value, prefer another tool or ask for clarification rather than guessing.
												 
												 Output rules:
												 - You must call the appropriate tool before answering.
												 - If the tool returns one or more stores, list every returned store name explicitly.
												 - Do not summarize away store names.
												 - If store ids are returned, include them.
												 - If cuisines are returned, include them.
												 - If no matching stores are returned, reply exactly: "No matching stores were found in the QuickBite catalog."
												 - Do not claim "no results" if a tool returned stores.
												 
												 Grounding rules:
												 - You must use the available tools to retrieve catalog data.
												 - Return only stores and menu items actually returned by the tools.
												 - Do not invent, assume, infer, complete, or suggest store names that were not returned.
												 - Do not use general world knowledge for stores or menus.
												 
												 User question: %s
												 """;

	public String create(FoodFlowContext context) {
		var extraction = context.getExtraction();

		return CATALOG_PROMPT.formatted(
				context.getCatalogSearchMode().name(),
				valueOrUnknown(extraction.cuisine()),
				valueOrUnknown(extraction.category()),
				valueOrUnknown(extraction.itemName()),
				valueOrUnknown(extraction.storeName()),
				valueOrUnknown(extraction.menuRequest()),
				valueOrUnknown(extraction.availableOnly()),
				enumValues(Cuisine.values()),
				enumValues(MenuItemCategory.values()),
				context.getQuestion());
	}

	private String enumValues(Enum<?>[] values) {
		return Arrays.stream(values)
					 .map(Enum::name)
					 .collect(Collectors.joining(", "));
	}

	private String valueOrUnknown(Object value) {
		return value == null ? "unknown" : String.valueOf(value);
	}
}