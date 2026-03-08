package gr.codelearn.spring.ai.food;

import java.util.Map;

public record Answer(String answer,
					 Integer promptTokens,
					 Integer completionTokens,
					 Integer totalTokens,
					 Long responseTimeMs,
					 Map<String, Object> modelMetadata) {
}