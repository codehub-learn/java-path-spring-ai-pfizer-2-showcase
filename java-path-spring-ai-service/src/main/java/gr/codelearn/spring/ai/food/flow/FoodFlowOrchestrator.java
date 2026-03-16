package gr.codelearn.spring.ai.food.flow;

import gr.codelearn.spring.ai.Key;
import gr.codelearn.spring.ai.food.Answer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class FoodFlowOrchestrator {
	private final @Qualifier("foodSupportChatClient") ChatClient foodSupportChatClient;
	private final @Qualifier("foodCatalogChatClient") ChatClient foodCatalogChatClient;

	private final IntentClassificationStep intentClassificationStep;
	private final QueryExtractionStep queryExtractionStep;
	private final CatalogPlanningStep catalogPlanningStep;

	private final SupportPromptFactory supportPromptFactory;
	private final CatalogPromptFactory catalogPromptFactory;
	private final FoodFlowAnswerAssembler answerAssembler;

	public Answer handle(String question, Key key) {
		FoodFlowContext context = new FoodFlowContext(question, key);

		try {
			// Step 1: classify intent.
			intentClassificationStep.apply(context);

			// Step 2: extract structured signals.
			queryExtractionStep.apply(context);

			// Step 3: plan catalog execution and clarification needs.
			catalogPlanningStep.apply(context);

			log.debug(
					"Food flow -> intent: {}, extraction: {}, catalogMode: {}, clarificationNeeded: {}",
					context.getIntent(),
					context.getExtraction(),
					context.getCatalogSearchMode(),
					context.isCatalogClarificationNeeded());

			return switch (context.getIntent()) {
				case SUPPORT -> handleSupport(context);
				case CATALOG -> handleCatalog(context);
				case BOTH -> handleBoth(context);
			};
		} catch (Exception e) {
			log.error("FoodFlowOrchestrator.handle failed for question='{}'.", question, e);
			return answerAssembler.error(
					"Sorry, I could not process your request right now. Please try again.",
					answerAssembler.elapsedMillis(context.getStartedAt()),
					Map.of("error", e.getClass().getSimpleName()));
		}
	}

	private Answer handleSupport(FoodFlowContext context) {
		return answerAssembler.success(
				call(foodSupportChatClient, supportPromptFactory.create(context), context.getKey()),
				answerAssembler.elapsedMillis(context.getStartedAt()));
	}

	private Answer handleCatalog(FoodFlowContext context) {
		if (context.isCatalogClarificationNeeded()) {
			return answerAssembler.clarification(
					"Please tell me what you want to search for in the catalog: cuisine, category, menu item, or store name.",
					answerAssembler.elapsedMillis(context.getStartedAt()),
					context.getExtraction());
		}

		return answerAssembler.success(
				call(foodCatalogChatClient, catalogPromptFactory.create(context), context.getKey()),
				answerAssembler.elapsedMillis(context.getStartedAt()));
	}

	private Answer handleBoth(FoodFlowContext context) {
		try {
			context.setSupportResponse(call(foodSupportChatClient, supportPromptFactory.create(context), context.getKey()));
		} catch (Exception e) {
			context.setSupportError(e);
			log.warn("Support branch failed for question='{}'.", context.getQuestion(), e);
		}

		if (!context.isCatalogClarificationNeeded()) {
			try {
				context.setCatalogResponse(call(foodCatalogChatClient, catalogPromptFactory.create(context), context.getKey()));
			} catch (Exception e) {
				context.setCatalogError(e);
				log.warn("Catalog branch failed for question='{}'.", context.getQuestion(), e);
			}
		}

		long responseTimeMs = answerAssembler.elapsedMillis(context.getStartedAt());

		if (context.getSupportResponse() != null && context.getCatalogResponse() != null) {
			return answerAssembler.combined(context.getSupportResponse(), context.getCatalogResponse(), responseTimeMs);
		}

		if (context.getSupportResponse() != null && context.isCatalogClarificationNeeded()) {
			Answer answer = answerAssembler.success(context.getSupportResponse(), responseTimeMs);
			return answerAssembler.supportWithCatalogClarification(answer);
		}

		if (context.getSupportResponse() != null) {
			Answer answer = answerAssembler.success(context.getSupportResponse(), responseTimeMs);
			return answerAssembler.partialSupport(answer, context.getCatalogError());
		}

		if (context.getCatalogResponse() != null) {
			Answer answer = answerAssembler.success(context.getCatalogResponse(), responseTimeMs);
			return answerAssembler.partialCatalog(answer, context.getSupportError());
		}

		return answerAssembler.error(
				"Sorry, I could not retrieve either support or catalog information right now. Please try again.",
				responseTimeMs,
				Map.of(
						"supportError", errorName(context.getSupportError()),
						"catalogError",
						context.isCatalogClarificationNeeded() ? "clarification_needed" : errorName(context.getCatalogError())
					  ));
	}

	private ChatResponse call(ChatClient chatClient, String prompt, Key key) {
		ChatResponse chatResponse = chatClient.prompt()
											  .user(prompt)
											  .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, key.toString()))
											  .call()
											  .chatResponse();

		return Objects.requireNonNull(chatResponse);
	}

	private String errorName(Exception error) {
		return error == null ? null : error.getClass().getSimpleName();
	}
}