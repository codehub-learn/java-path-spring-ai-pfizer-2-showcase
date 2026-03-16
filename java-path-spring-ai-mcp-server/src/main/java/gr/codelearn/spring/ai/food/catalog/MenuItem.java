package gr.codelearn.spring.ai.food.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "food_menu_item")
@Getter
@ToString(exclude = "storeMenu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 120, nullable = false)
	private String name;

	@Column(length = 500, nullable = false)
	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(length = 80, nullable = false)
	private MenuItemCategory category;

	@Column(nullable = false)
	private boolean available;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_menu_id", nullable = false)
	private StoreMenu storeMenu;

	public MenuItem(String name, String description, BigDecimal price, MenuItemCategory category, boolean available) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.category = category;
		this.available = available;
	}

	void setStoreMenu(StoreMenu storeMenu) {
		this.storeMenu = storeMenu;
	}
}