package gr.codelearn.spring.ai.food.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "food_store")
@Getter
@ToString(exclude = "storeMenu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {
	@Id
	@Column(length = 64, nullable = false)
	private String id;

	@Column(length = 120, nullable = false)
	private String name;

	@Column(nullable = false)
	private boolean openNow;

	@OneToOne(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
	private StoreMenu storeMenu;

	public Store(String id, String name, boolean openNow) {
		this.id = id;
		this.name = name;
		this.openNow = openNow;
	}

	public void setStoreMenu(StoreMenu storeMenu) {
		this.storeMenu = storeMenu;
		storeMenu.setStore(this);
	}
}