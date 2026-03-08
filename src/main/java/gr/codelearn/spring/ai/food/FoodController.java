package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.Key;
import gr.codelearn.spring.ai.food.catalog.StoreCatalogService;
import gr.codelearn.spring.ai.food.catalog.StoreMenuResource;
import gr.codelearn.spring.ai.food.catalog.StoreSummaryResource;
import gr.codelearn.spring.ai.food.rag.RagEtlService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/{tenant}/{user}/food")
public class FoodController {
	private final RagEtlService etlService;
	private final FoodService foodService;
	private final StoreCatalogService storeCatalogService;

	@PostMapping(headers = "x-reindex")
	public RagEtlService.EtlResult reIndex() throws Exception {
		return etlService.reindex();
	}

	@GetMapping(params = {"q", "k"})
	public List<Map<String, Object>> search(@RequestParam("q") String query, @RequestParam(value = "k", defaultValue = "5") int topK) {
		List<Document> docs = etlService.search(query, topK);

		return docs.stream().map(d -> Map.of("id", d.getId(),
											 "metadata", d.getMetadata(),
											 "preview", preview(d.getText(), 280)))
				   .toList();
	}

	@GetMapping(params = {"qu"})
	public Answer ask(@PathVariable String tenant, @PathVariable String user, @RequestParam("qu") String question) {
		return foodService.ask(question, new Key(tenant, user, "food_ordering"));
	}

	@GetMapping(path = "/stores", params = "q")
	public List<StoreSummaryResource> findStores(@RequestParam("q") String query) {
		return storeCatalogService.findStores(query);
	}

	@GetMapping(path = "/stores/{storeId}/menu")
	public StoreMenuResource getStoreMenu(@PathVariable String storeId) {
		return storeCatalogService.getStoreMenu(storeId);
	}

	private String preview(String s, int max) {
		if (s == null) {
			return "";
		}
		String oneLine = s.replace("\r", "").replace("\n", " ").trim();
		if (oneLine.length() <= max) {
			return oneLine;
		}
		return oneLine.substring(0, max) + "...";
	}
}