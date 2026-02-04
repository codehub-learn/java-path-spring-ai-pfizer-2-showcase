package gr.codelearn.spring.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class SpringAiDemo {
	static void main(String[] args) {
		SpringApplication.run(SpringAiDemo.class, args);
	}

	@Bean
	CommandLineRunner askOllama(ChatClient.Builder chatClientBuilder) {
		return _ -> {
			ChatClient chatClient = chatClientBuilder.build();

			String answer = chatClient
					.prompt("""
							List the 10 biggest cities in Greece by population.
							Return as a numbered list with: City — short note (max 8 words).
							""")
					.call()
					.content();

			IO.println("Ollama says:\n" + answer);

			Flux<String> chunks = chatClient
					.prompt("""
							List the 30 biggest cities in the world by population. Do not return duplicates.
							Return as a numbered list with: City — population.
							Make sure to update with the latest information.
							""")
					.stream()
					.content();

			chunks.doOnNext(IO::print)     // print tokens as they arrive
				  .doOnError(e -> System.err.println("\nStreaming error: " + e.getMessage()))
				  .doOnComplete(() -> IO.println("\n\n(done)"))
				  .blockLast();
		};
	}
}