package gr.codelearn.spring.ai.food.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, String> {
	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.menuItems", "storeMenu.cuisines"})
	Optional<Store> findWithStoreMenuById(String id);

	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.cuisines"})
	List<Store> findByNameContainingIgnoreCaseOrderByNameAsc(String title);

	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.cuisines"})
	List<Store> findAllByOrderByNameAsc();

	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.cuisines"})
	@Query("""
		   select distinct s
		   from Store s
		   join s.storeMenu sm
		   join sm.cuisines c
		   where c = :cuisine
		   order by s.name
		   """)
	List<Store> findByCuisineOrderByNameAsc(Cuisine cuisine);

	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.cuisines"})
	@Query("""
		   select distinct s
		   from Store s
		   join s.storeMenu sm
		   join sm.menuItems mi
		   where mi.category = :category
		   order by s.name
		   """)
	List<Store> findByMenuItemCategoryOrderByNameAsc(MenuItemCategory category);

	@EntityGraph(attributePaths = {"storeMenu", "storeMenu.cuisines"})
	@Query("""
		   select distinct s
		   from Store s
		   join s.storeMenu sm
		   join sm.menuItems mi
		   where lower(mi.name) like lower(concat('%', :itemName, '%'))
		   order by s.name
		   """)
	List<Store> findByMenuItemNameContainingIgnoreCaseOrderByNameAsc(String itemName);
}