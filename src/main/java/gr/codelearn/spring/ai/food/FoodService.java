package gr.codelearn.spring.ai.food;

import gr.codelearn.spring.ai.Key;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Service
public class FoodService {
	@Qualifier("foodChatClient")
	private final ChatClient foodChatClient;

	public Flux<String> ask(String question, final Key key) {
		return foodChatClient.prompt()
							 .user(question)
							 // Adds data to the advisor context
							 .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, key.toString()))
							 .stream()
							 .content();
	}
}