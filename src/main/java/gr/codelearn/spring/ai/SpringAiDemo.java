package gr.codelearn.spring.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
		};
	}
}