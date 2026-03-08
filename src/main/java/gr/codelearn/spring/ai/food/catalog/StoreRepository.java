package gr.codelearn.spring.ai.food.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, String> {
	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.menuItems", "storeMenu.cuisines"})
	Optional<Store> findWithStoreMenuById(String id);

	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.cuisines"})
	List<Store> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.cuisines"})
	List<Store> findAllByOrderByNameAsc();
}