package gr.codelearn.spring.ai.chat;

import gr.codelearn.spring.ai.Key;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ChatService {
	private final ChatClient chatClient;
	private final ChatMemoryRepository chatMemoryRepository;

	public Flux<String> ask(final Key key, String question) {
		return chatClient
				.prompt()
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, key.toString()))
				.user(question)
				.stream()
				.content();
	}

	public List<ChatController.ChatMessageDto> getConversation(final Key key) {
		int limit = 1_000;

		return chatMemoryRepository.findByConversationId(key.toString()).stream()
								   .limit(limit)
								   .map(this::toDto)
								   .toList();
	}

	private String conversationKey(String tenant, String user, String conversationId) {
		return tenant + ":" + user + ":" + conversationId;
	}

	private ChatController.ChatMessageDto toDto(org.springframework.ai.chat.messages.Message message) {
		String role;
		if (message instanceof UserMessage) {
			role = "user";
		} else if (message instanceof AssistantMessage) {
			role = "assistant";
		} else if (message instanceof SystemMessage) {
			role = "system";
		} else {
			role = "unknown";
		}
		return new ChatController.ChatMessageDto(role, message.getText());
	}

}