package gr.codelearn.spring.ai.food.flow;

import gr.codelearn.spring.ai.food.Answer;
import gr.codelearn.spring.ai.food.FoodQueryExtraction;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class FoodFlowAnswerAssembler {
	public Answer success(ChatResponse chatResponse, long responseTimeMs) {
		String text = Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();

		return new Answer(
				text,
				chatResponse.getMetadata().getUsage().getPromptTokens(),
				chatResponse.getMetadata().getUsage().getCompletionTokens(),
				chatResponse.getMetadata().getUsage().getTotalTokens(),
				responseTimeMs,
				chatResponse.getResult().getOutput().getMetadata()
		);
	}

	public Answer clarification(String message, long responseTimeMs, FoodQueryExtraction extraction) {
		return new Answer(
				message,
				0,
				0,
				0,
				responseTimeMs,
				Map.of(
						"flow", "multi-step",
						"status", "clarification_needed",
						"extraction", extraction
					  )
		);
	}

	public Answer combined(ChatResponse supportResponse, ChatResponse catalogResponse, long responseTimeMs) {
		String supportText = Objects.requireNonNull(supportResponse.getResult()).getOutput().getText();
		String catalogText = Objects.requireNonNull(catalogResponse.getResult()).getOutput().getText();

		String combinedText = """
							  Support information:
							  %s
							  
							  Catalog information:
							  %s
							  """.formatted(supportText, catalogText);

		Integer promptTokens = sum(supportResponse.getMetadata().getUsage().getPromptTokens(),
								   catalogResponse.getMetadata().getUsage().getPromptTokens());
		Integer completionTokens = sum(supportResponse.getMetadata().getUsage().getCompletionTokens(),
									   catalogResponse.getMetadata().getUsage().getCompletionTokens());
		Integer totalTokens = sum(supportResponse.getMetadata().getUsage().getTotalTokens(),
								  catalogResponse.getMetadata().getUsage().getTotalTokens());

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("support", supportResponse.getResult().getOutput().getMetadata());
		metadata.put("catalog", catalogResponse.getResult().getOutput().getMetadata());

		return new Answer(
				combinedText,
				promptTokens,
				completionTokens,
				totalTokens,
				responseTimeMs,
				metadata
		);
	}

	public Answer partialSupport(Answer answer, Exception catalogError) {
		return new Answer(
				answer.answer() + "\n\nCatalog information could not be retrieved at the moment.",
				answer.promptTokens(),
				answer.completionTokens(),
				answer.totalTokens(),
				answer.responseTimeMs(),
				appendErrorMetadata(answer.modelMetadata(), "catalog", catalogError)
		);
	}

	public Answer partialCatalog(Answer answer, Exception supportError) {
		return new Answer(
				"Support information could not be retrieved at the moment.\n\n" + answer.answer(),
				answer.promptTokens(),
				answer.completionTokens(),
				answer.totalTokens(),
				answer.responseTimeMs(),
				appendErrorMetadata(answer.modelMetadata(), "support", supportError)
		);
	}

	public Answer supportWithCatalogClarification(Answer answer) {
		return new Answer(
				answer.answer() + "\n\nIf you also want catalog results, tell me a cuisine, category, menu item, or store name.",
				answer.promptTokens(),
				answer.completionTokens(),
				answer.totalTokens(),
				answer.responseTimeMs(),
				appendBranchMetadata(answer.modelMetadata(), "catalogStatus", "clarification_needed")
		);
	}

	public Answer error(String message, long responseTimeMs, Map<String, Object> metadata) {
		return new Answer(message, 0, 0, 0, responseTimeMs, metadata);
	}

	public long elapsedMillis(Instant startedAt) {
		return Duration.between(startedAt, Instant.now()).toMillis();
	}

	private Integer sum(Integer left, Integer right) {
		int safeLeft = left == null ? 0 : left;
		int safeRight = right == null ? 0 : right;
		return safeLeft + safeRight;
	}

	private Map<String, Object> appendErrorMetadata(Map<String, Object> metadata, String branch, Exception error) {
		Map<String, Object> enriched = new LinkedHashMap<>();
		if (metadata != null) {
			enriched.putAll(metadata);
		}
		enriched.put(branch + "Error", errorName(error));
		return enriched;
	}

	private Map<String, Object> appendBranchMetadata(Map<String, Object> metadata, String key, String value) {
		Map<String, Object> enriched = new LinkedHashMap<>();
		if (metadata != null) {
			enriched.putAll(metadata);
		}
		enriched.put(key, value);
		return enriched;
	}

	private String errorName(Exception error) {
		return error == null ? null : error.getClass().getSimpleName();
	}
}