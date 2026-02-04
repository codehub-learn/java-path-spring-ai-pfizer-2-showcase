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

			String answer = chatClient.prompt("What is the capital of Greece? Answer in one word.").call().content();

			IO.println("Ollama says: " + answer);
		};
	}
}