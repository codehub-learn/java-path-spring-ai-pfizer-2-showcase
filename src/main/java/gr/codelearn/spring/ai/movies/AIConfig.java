package gr.codelearn.spring.ai.movies;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
	@Bean
	public ChatClient moviesChatClient(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder
				.defaultSystem("""
							   You are a helpful CLI assistant. You are expert in movies.
							   Keep answers concise.
							   When asked for a list of things, use a numbered list.
							   """)
				.build();
	}
}