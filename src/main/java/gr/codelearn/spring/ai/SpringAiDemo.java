package gr.codelearn.spring.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SpringBootApplication
public class SpringAiDemo {
	static void main(String[] args) {
		SpringApplication.run(SpringAiDemo.class, args);
	}

	@Bean
	CommandLineRunner askOllamaViaChatClient(ChatClient.Builder chatClientBuilder) {
		return _ -> {
			var chatClient = chatClientBuilder
					.defaultSystem("When asked for a list of data, never return duplicates and always return a numbered list.")
					.build();

			var chatResponse = chatClient
					.prompt("List the 20 biggest cities in Europe by population. Return city and population.")
					.call()
					.chatResponse();
			log.info("Ollama says: {}", chatResponse.getResult().getOutput().getText());

			var metadata = chatResponse.getMetadata();
			log.trace("Usage: {}.", metadata.getUsage());
			//I have no rate limit when using Ollama
			//chatResponse.getMetadata().getRateLimit();
			log.trace("Total duration: {}.", Optional.ofNullable(metadata.get("total-duration")));

			Flux<String> chunks = chatClient
					.prompt("List the 20 biggest cities in US (metro area) by population. Return city and population.")
					.stream()
					.content();
			chunks.doOnNext(IO::print)
				  .doOnError(e -> System.err.println("Streaming error: " + e.getMessage()))
				  .doOnComplete(() -> IO.println("\n\nDone."))
				  .blockLast();

			StringBuilder fullResponse = new StringBuilder();
			Flux<ChatResponse> responses = chatClient
					.prompt("List the 20 highest mountains in the worlds. Return mountain, country and height in meters.")
					.stream()
					.chatResponse();

			AtomicReference<ChatResponseMetadata> responseMetadata = new AtomicReference<>();
			responses.doOnNext(cr -> {
						 fullResponse.append(cr.getResult().getOutput().getText());
						 if (cr.getMetadata() != null) {
							 responseMetadata.set(cr.getMetadata());
						 }
					 })
					 .doOnError(e -> log.error("Streaming error: {}", e.getMessage(), e))
					 .doOnComplete(() -> {
						 log.info("Streaming complete. Full response: \n{}", fullResponse);
						 log.debug("Usage, prompt tokens:{}, completion tokens:{}, total tokens:{}.",
								   responseMetadata.get().getUsage().getPromptTokens(),
								   responseMetadata.get().getUsage().getCompletionTokens(),
								   responseMetadata.get().getUsage().getTotalTokens());
						 log.debug("Total duration: {}.", responseMetadata.get().get("total-duration").toString());
						 log.debug("Done.");
					 })
					 .blockLast();

			var x = chatResponse.getMetadata().getUsage();
			var y = chatResponse.getMetadata().getRateLimit();
			var z = chatResponse.getMetadata().get("total-duration");
		};
	}
}