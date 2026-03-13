package gr.codelearn.spring.ai.food.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StoreCatalogService {
	private final StoreRepository storeRepository;

	@Tool(name = "find-stores-by-title",
		  description =
				  "Find QuickBite stores by full or partial store title. Use this when the user mentions a store name or part of it," +
				  " for example Pizza, Sakura, Burger, Tokyo.")
	public List<StoreSummaryResource> findStoresByTitle(String title) {
		log.debug("StoreCatalogService.findStoresByTitle called with title '{}'.", title);
		List<Store> stores = StringUtils.hasText(title)
							 ? storeRepository.findByNameContainingIgnoreCaseOrderByNameAsc(title.trim())
							 : storeRepository.findAllByOrderByNameAsc();

		return stores.stream()
					 .map(this::toSummaryResource)
					 .toList();
	}

	@Tool(name = "find-stores-by-cuisine",
		  description = "Find QuickBite stores by cuisine. Use this for requests such as sushi, japanese, pizza, italian, burgers, " +
						"mexican, vegan, or desserts. The input must be a Cuisine enum value.")
	public List<StoreSummaryResource> findStoresByCuisine(Cuisine cuisine) {
		log.debug("StoreCatalogService.findStoresByCuisine called with cuisine '{}'.", cuisine);
		return storeRepository.findByCuisineOrderByNameAsc(cuisine)
							  .stream()
							  .map(this::toSummaryResource)
							  .toList();
	}

	@Tool(name = "find-stores-by-menu-item-category",
		  description = "Find QuickBite stores by menu item category. Use this when the user asks for stores serving a category such as " +
						"SUSHI, PIZZA, BURGERS, DESSERTS, DRINKS, TACOS, or PASTA. The input must be a MenuItemCategory enum value.")
	public List<StoreSummaryResource> findStoresByMenuItemCategory(MenuItemCategory category) {
		log.debug("StoreCatalogService.findStoresByMenuItemCategory called with category '{}'.", category);
		return storeRepository.findByMenuItemCategoryOrderByNameAsc(category)
							  .stream()
							  .map(this::toSummaryResource)
							  .toList();
	}

	@Tool(name = "find-stores-by-menu-item-name",
		  description = "Find QuickBite stores by full or partial menu item name. Use this for specific dishes such as nigiri, tiramisu," +
						" " +
						"churros, pepperoni, brownie, burrito, or mochi.")
	public List<StoreSummaryResource> findStoresByMenuItemName(String itemName) {
		log.debug("StoreCatalogService.findStoresByMenuItemName called with itemName '{}'.", itemName);
		if (!StringUtils.hasText(itemName)) {
			return List.of();
		}

		return storeRepository.findByMenuItemNameContainingIgnoreCaseOrderByNameAsc(itemName.trim())
							  .stream()
							  .map(this::toSummaryResource)
							  .toList();
	}

	@Tool(name = "get-store-menu",
		  description = "Get the full menu for a specific QuickBite store using its storeId.")
	public StoreMenuResource getStoreMenu(String storeId) {
		log.debug("StoreCatalogService.getStoreMenu called with storeId '{}'.", storeId);
		Store store = storeRepository.findWithStoreMenuById(storeId)
									 .orElseThrow(() -> new IllegalArgumentException("Unknown storeId: " + storeId));
		return toStoreMenuResource(store);
	}

	private StoreSummaryResource toSummaryResource(Store store) {
		return new StoreSummaryResource(
				store.getId(),
				store.getName(),
				new LinkedHashSet<>(store.getStoreMenu().getCuisines()),
				store.isOpenNow());
	}

	private StoreMenuResource toStoreMenuResource(Store store) {
		List<MenuItemResource> items = store.getStoreMenu()
											.getMenuItems()
											.stream()
											.map(this::toMenuItemResource)
											.toList();

		return new StoreMenuResource(
				store.getId(),
				store.getName(),
				new LinkedHashSet<>(store.getStoreMenu().getCuisines()),
				store.isOpenNow(),
				items);
	}

	private MenuItemResource toMenuItemResource(MenuItem item) {
		return new MenuItemResource(
				item.getName(),
				item.getDescription(),
				item.getPrice(),
				item.getCategory(),
				item.isAvailable());
	}
}