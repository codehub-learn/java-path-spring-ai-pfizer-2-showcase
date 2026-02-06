package gr.codelearn.spring.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SpringBootApplication
public class SpringAiDemo {
	static void main(String[] args) {
		SpringApplication.run(SpringAiDemo.class, args);
	}

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
						 log.trace("Usage, prompt tokens:{}, completion tokens:{}, total tokens:{}.",
								   responseMetadata.get().getUsage().getPromptTokens(),
								   responseMetadata.get().getUsage().getCompletionTokens(),
								   responseMetadata.get().getUsage().getTotalTokens());
						 log.trace("Total duration: {}.", responseMetadata.get().get("total-duration").toString());
						 log.debug("Done.\n\n");
					 })
					 .blockLast();
		};
	}

	CommandLineRunner askOllamaViaChatModel(ChatModel chatModel) {
		return _ -> {
			var response = chatModel.call(new Prompt("Give me 5 practical tips for improving Java performance in no more than 30 words " +
													 "per item. Return a numbered list."));
			log.info("Ollama says:\n{}", response.getResult().getOutput().getText());

			var metadata = response.getMetadata();
			log.trace("Usage: {}", metadata.getUsage());
			log.trace("Total duration: {}.", metadata.get("total-duration").toString());
		};
	}

	@Bean
	CommandLineRunner askOllamaInteractivelyViaChatModel(ChatModel chatModel) {
		return _ -> {
			try (Scanner scanner = new Scanner(System.in)) {
				IO.println("Interactive AI-based Ollama CLI (type 'exit' to quit)");
				while (true) {
					IO.print("\nYou> ");
					String userInput = scanner.nextLine();
					if (userInput == null) {
						break;
					}

					userInput = userInput.trim();
					if (userInput.isEmpty()) {
						continue;
					}

					if ("exit" .equalsIgnoreCase(userInput) || "quit" .equalsIgnoreCase(userInput)) {
						IO.println("Bye.");
						break;
					}

					try {
						var response = chatModel.call(new Prompt(userInput));
						IO.println("\nAI> " + response.getResult().getOutput().getText());

						var metadata = response.getMetadata();
						log.trace("Usage, prompt tokens:{}, completion tokens:{}, total tokens:{}.",
								  metadata.getUsage().getPromptTokens(),
								  metadata.getUsage().getCompletionTokens(),
								  metadata.getUsage().getTotalTokens());
						log.trace("Total duration: {}", Optional.ofNullable(metadata.get("total-duration")));
					} catch (Exception e) {
						log.error("ChatModel call failed: {}", e.getMessage(), e);
						IO.println("Error: " + e.getMessage());
					}
				}
			}
		};
	}
}