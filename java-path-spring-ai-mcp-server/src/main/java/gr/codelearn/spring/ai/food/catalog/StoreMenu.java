package gr.codelearn.spring.ai.food.catalog;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "food_store_menu")
@Getter
@ToString(exclude = {"store", "menuItems"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreMenu {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false, unique = true)
	private Store store;

	@ElementCollection(targetClass = Cuisine.class)
	@CollectionTable(name = "food_store_menu_cuisine", joinColumns = @JoinColumn(name = "store_menu_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "cuisine", length = 80, nullable = false)
	private Set<Cuisine> cuisines = new LinkedHashSet<>();

	@OneToMany(mappedBy = "storeMenu", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("name asc")
	private final List<MenuItem> menuItems = new ArrayList<>();

	public StoreMenu(Set<Cuisine> cuisines) {
		this.cuisines = new LinkedHashSet<>(cuisines);
	}

	public void addMenuItem(MenuItem menuItem) {
		menuItem.setStoreMenu(this);
		this.menuItems.add(menuItem);
	}

	void setStore(Store store) {
		this.store = store;
	}
}