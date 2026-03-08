package gr.codelearn.spring.ai.food.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StoreCatalogService {
	private final StoreRepository storeRepository;

	@Tool(name = "find-stores",
		  description = "Find stores registered to QuickBite platform by title or part of the title. More than one store can match " +
						"the given query.")
	public List<StoreSummaryResource> findStores(String query) {
		List<Store> stores = StringUtils.hasText(query)
							 ? storeRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim())
							 : storeRepository.findAllByOrderByNameAsc();

		return stores.stream()
					 .map(this::toSummaryResource)
					 .toList();
	}

	@Tool(name = "get-stores-menu",
		  description = "Get the full menu for a specific store registered to QuickBite platform using its storeId.")
	public StoreMenuResource getStoreMenu(String storeId) {
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