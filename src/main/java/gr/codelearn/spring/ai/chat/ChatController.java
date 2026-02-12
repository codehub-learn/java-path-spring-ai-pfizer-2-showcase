package gr.codelearn.spring.ai.chat;

import gr.codelearn.spring.ai.Key;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/{tenant}/{user}/chat")
public class ChatController {
	private final ChatService chatService;

	@PostMapping(
			value = "/{conversationId}/ask",
			produces = MediaType.TEXT_EVENT_STREAM_VALUE
	)
	public Flux<String> ask(@PathVariable String tenant, @PathVariable String user, @PathVariable String conversationId,
							@RequestBody AskRequest request) {
		return chatService.ask(new Key(tenant, user, conversationId), request.question());
	}

	@GetMapping(value = "/{conversationId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ChatMessageDto> messages(@PathVariable String tenant, @PathVariable String user, @PathVariable String conversationId) {
		return chatService.getConversation(new Key(tenant, user, conversationId));
	}

	public record AskRequest(String question) {
	}

	public record ChatMessageDto(String role, String content) {
	}
}