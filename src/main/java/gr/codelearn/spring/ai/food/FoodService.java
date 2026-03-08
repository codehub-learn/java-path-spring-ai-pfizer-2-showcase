package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.Key;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class FoodService {
	@Qualifier("foodChatClient")
	private final ChatClient foodChatClient;

	public Answer ask(String question, final Key key) {
		var start = Instant.now();
		var chatResponse = foodChatClient.prompt()
										 .user(question)
										 // Adds data to the advisor context
										 .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, key.toString()))
										 .call()
										 .chatResponse();
		assert chatResponse != null;

		return new Answer(Objects.requireNonNull(chatResponse.getResult()).getOutput().getText(),
						  chatResponse.getMetadata().getUsage().getPromptTokens(),
						  chatResponse.getMetadata().getUsage().getCompletionTokens(),
						  chatResponse.getMetadata().getUsage().getTotalTokens(),
						  Duration.between(start, Instant.now()).toMillis(),
						  chatResponse.getResult().getOutput().getMetadata());
	}
}