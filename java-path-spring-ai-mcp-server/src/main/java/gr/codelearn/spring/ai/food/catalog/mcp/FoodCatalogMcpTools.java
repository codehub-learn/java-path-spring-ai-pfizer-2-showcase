package gr.codelearn.spring.ai.food.catalog.mcp;

import gr.codelearn.spring.ai.food.catalog.StoreCatalogService;
import gr.codelearn.spring.ai.food.catalog.StoreMenuResource;
import gr.codelearn.spring.ai.food.catalog.StoreSummaryResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class FoodCatalogMcpTools {
	private final StoreCatalogService storeCatalogService;

	@McpTool(name = "find_stores_by_title",
			 description = "Find QuickBite stores by title or partial store name.")
	public List<StoreSummaryResource> findStoresByTitle(
			@McpToolParam(description = "Store title or partial store title") String title) {
		log.debug("MCP tool find_stores_by_title called with title='{}'.", title);
		return storeCatalogService.findStoresByTitle(title);
	}

	@McpTool(
			name = "find_stores_by_cuisine",
			description =
					"Find QuickBite stores by cuisine. Supported values include JAPANESE, ITALIAN, MEXICAN, SUSHI, VEGAN, DESSERTS, " +
					"and other valid cuisine enum values."
	)
	public List<StoreSummaryResource> findStoresByCuisine(
			@McpToolParam(description = "Cuisine name such as JAPANESE, ITALIAN, MEXICAN, SUSHI, VEGAN, or DESSERTS")
			String cuisine
														 ) {
		log.debug("MCP tool find_stores_by_cuisine called with cuisine='{}'.", cuisine);
		return storeCatalogService.findStoresByCuisine(cuisine);
	}

	@McpTool(
			name = "find_stores_by_menu_item_category",
			description = "Find QuickBite stores by menu item category."
	)
	public List<StoreSummaryResource> findStoresByMenuItemCategory(
			@McpToolParam(description = "Menu item category value")
			String category
																  ) {
		log.debug("MCP tool find_stores_by_menu_item_category called with category='{}'.", category);
		return storeCatalogService.findStoresByMenuItemCategory(category);
	}

	@McpTool(
			name = "find_stores_by_menu_item_name",
			description = "Find QuickBite stores by menu item name such as burrito, tiramisu, coffee, brownie, maki, or pizza."
	)
	public List<StoreSummaryResource> findStoresByMenuItemName(
			@McpToolParam(description = "Menu item name")
			String itemName
															  ) {
		log.debug("MCP tool find_stores_by_menu_item_name called with itemName='{}'.", itemName);
		return storeCatalogService.findStoresByMenuItemName(itemName);
	}

	@McpTool(
			name = "get_store_menu",
			description = "Get the full menu for a QuickBite store using the store identifier."
	)
	public StoreMenuResource getStoreMenu(
			@McpToolParam(description = "Store identifier")
			String storeId
										 ) {
		log.debug("MCP tool get_store_menu called with storeId='{}'.", storeId);
		return storeCatalogService.getStoreMenu(storeId);
	}
}