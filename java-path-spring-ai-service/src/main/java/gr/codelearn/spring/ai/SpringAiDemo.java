package gr.codelearn.spring.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SpringBootApplication
public class SpringAiDemo {
	static void main(String[] args) {
		SpringApplication.run(SpringAiDemo.class, args);
	}

	CommandLineRunner askOllamaUsingPromptTemplate(ChatClient.Builder chatClientBuilder) {
		return _ -> {
			var chatClient = chatClientBuilder
					.defaultSystem("""
								   You are a helpful CLI assistant.
								   Keep answers concise.
								   When giving steps, use a numbered list.
								   """)
					.build();

			String template = """
							  Create a {style} explanation about: {topic}
							  Audience: {audience}
							  Constraints:
							  - Provide exactly {count} bullet points
							  - Each bullet must start with an imperative verb
							  """;

			// Values to inject
			Map<String, Object> vars = Map.of(
					"style", "practical",
					"topic", "Java memory leaks",
					"audience", "mid-level backend developers",
					"count", 5
											 );
			ChatResponse response = chatClient
					.prompt()
					.user(u -> u.text(template).params(vars))
					.call()
					.chatResponse();

			log.info("Template result:\n{}", response.getResult().getOutput().getText());
		};
	}

	CommandLineRunner askOllamaUsingFewShotPrompting(ChatClient.Builder chatClientBuilder) {
		return _ -> {
			var chatClient = chatClientBuilder
					.defaultSystem("""
								   You are a helpful CLI assistant.
								   Keep answers concise.
								   When giving steps, use a numbered list.
								   """)
					.build();

			Prompt fewShot = new Prompt(List.of(
					new SystemMessage("""
									  You transform short support messages into STRICT JSON with keys:
									  sentiment, summary, action_items.
									  Output ONLY JSON. No markdown.
									  """),

					// Shot 1 (user -> assistant)
					new UserMessage("Convert this text into the JSON format: \"Thanks, the issue is resolved quickly.\""),
					new AssistantMessage("""
										 {
										   "sentiment": "positive",
										   "summary": "Customer reports the issue was resolved quickly.",
										   "action_items": ["Confirm closure with the customer"]
										 }
										 """),

					// Shot 2 (user -> assistant)
					new UserMessage("Convert this text into the JSON format: \"The app keeps crashing after login. Please fix ASAP.\""),
					new AssistantMessage("""
										 {
										   "sentiment": "negative",
										   "summary": "User reports repeated crashes after login and requests an urgent fix.",
										   "action_items": ["Collect crash logs", "Reproduce the issue after login", "Prioritize a hotfix release"]
										 }
										 """),

					// Real question (only user; model completes with the next assistant message)
					new UserMessage("Convert this text into the JSON format: \"Checkout is slow on mobile, but it works. Any ideas?\"")
											   ));

			var chatResponse = chatClient
					.prompt(fewShot)
					.call()
					.chatResponse();

			log.info("Ollama says: {}", chatResponse.getResult().getOutput().getText());
			var metadata = chatResponse.getMetadata();
			log.trace("Usage: {}.", metadata.getUsage());
			//I have no rate limit when using Ollama
			//chatResponse.getMetadata().getRateLimit();
			log.trace("Total duration: {}.", Optional.ofNullable(metadata.get("total-duration")));
		};
	}

	CommandLineRunner askOllamaViaChatClient(ChatClient.Builder chatClientBuilder) {
		return _ -> {
			var chatClient = chatClientBuilder
					.defaultSystem("""
								   You are a helpful CLI assistant.
								   Keep answers concise.
								   When giving steps, use a numbered list.
								   """)
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

	CommandLineRunner askOllamaInteractivelyViaChatModel(ChatModel chatModel) {
		return _ -> {
			var systemInstructions = """
									 You are a helpful CLI assistant.
									 Be concise. When you provide steps, use a numbered list.
									 If you are unsure, say so and ask a clarifying question.
									 """;
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

					if ("exit".equalsIgnoreCase(userInput) || "quit".equalsIgnoreCase(userInput)) {
						IO.println("Bye.");
						break;
					}

					try {
						IO.print("\nAI> ");

						Prompt prompt = new Prompt(List.of(
								new SystemMessage(systemInstructions),
								new UserMessage(userInput)));

						var full = new StringBuilder();
						Flux<ChatResponse> events = chatModel.stream(prompt);

						events.doOnNext(cr -> {
								  String chunk = cr.getResult().getOutput().getText();
								  if (chunk != null && !chunk.isBlank()) {
									  full.append(chunk);
									  IO.print(chunk);
								  }
							  })
							  .doOnError(e -> {
								  log.error("Streaming error: {}", e.getMessage(), e);
								  IO.println("\nError: " + e.getMessage());
							  })
							  .doOnComplete(() -> {
								  IO.println("\n");
								  log.trace("Full response chars: {}", full.length());
							  })
							  .blockLast();

					} catch (Exception e) {
						log.error("Streaming call failed: {}", e.getMessage(), e);
						IO.println("Error: " + e.getMessage());
					}
				}
			}
		};
	}

	CommandLineRunner askOllamaInteractivelyViaChatClient(final ChatClient moviesChatClient) {
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

					if ("exit".equalsIgnoreCase(userInput) || "quit".equalsIgnoreCase(userInput)) {
						IO.println("Bye.");
						break;
					}

					try {
						IO.print("\nAI> ");

						Prompt prompt = new Prompt(List.of(new UserMessage(userInput)));

						var full = new StringBuilder();
						Flux<ChatResponse> events = moviesChatClient.prompt(prompt).stream().chatResponse();

						events.doOnNext(cr -> {
								  String chunk = cr.getResult().getOutput().getText();
								  if (chunk != null && !chunk.isBlank()) {
									  full.append(chunk);
									  IO.print(chunk);
								  }
							  })
							  .doOnError(e -> {
								  log.error("Streaming error: {}", e.getMessage(), e);
								  IO.println("\nError: " + e.getMessage());
							  })
							  .doOnComplete(() -> {
								  IO.println("\n");
								  log.trace("Full response chars: {}", full.length());
							  })
							  .blockLast();

					} catch (Exception e) {
						log.error("Streaming call failed: {}", e.getMessage(), e);
						IO.println("Error: " + e.getMessage());
					}
				}
			}
		};
	}

	@Bean
	CommandLineRunner askOllamaInteractivelyViaMcpChatClient(final ChatClient foodCatalogMcpChatClient) {
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

					if ("exit".equalsIgnoreCase(userInput) || "quit".equalsIgnoreCase(userInput)) {
						IO.println("Bye.");
						break;
					}

					try {
						IO.print("\nAI> ");

						Prompt prompt = new Prompt(List.of(new UserMessage(userInput)));

						var full = new StringBuilder();
						Flux<ChatResponse> events = foodCatalogMcpChatClient.prompt(prompt).stream().chatResponse();

						events.doOnNext(cr -> {
								  String chunk = cr.getResult().getOutput().getText();
								  if (chunk != null && !chunk.isBlank()) {
									  full.append(chunk);
									  IO.print(chunk);
								  }
							  })
							  .doOnError(e -> {
								  log.error("Streaming error: {}", e.getMessage(), e);
								  IO.println("\nError: " + e.getMessage());
							  })
							  .doOnComplete(() -> {
								  IO.println("\n");
								  log.trace("Full response chars: {}", full.length());
							  })
							  .blockLast();

					} catch (Exception e) {
						log.error("Streaming call failed: {}", e.getMessage(), e);
						IO.println("Error: " + e.getMessage());
					}
				}
			}
		};
	}
}