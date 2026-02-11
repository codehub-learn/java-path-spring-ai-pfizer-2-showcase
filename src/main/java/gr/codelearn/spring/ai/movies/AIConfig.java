package gr.codelearn.spring.ai.movies;

import io.netty.channel.ChannelOption;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class AIConfig {
	@Bean
	public ChatClient moviesChatClient(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder
				.defaultSystem("""
							   You are a helpful CLI assistant. You are expert in movies.
							   Keep answers concise.
							   When asked for a list of things, use a numbered list.
							   Always return the list of actors
							   """)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(getChatMemory()).build())
				.build();
	}

	public ChatMemory getChatMemory() {
		return MessageWindowChatMemory.builder().maxMessages(10).build();
	}

	@Bean
	public ConnectionProvider aiConnectionProvider() {
		return ConnectionProvider.builder("ai-http-pool")
								 .maxConnections(50)
								 .pendingAcquireTimeout(Duration.ofSeconds(10))
								 .maxIdleTime(Duration.ofSeconds(180))
								 .maxLifeTime(Duration.ofMinutes(20))
								 .build();
	}

	@Bean
	public ReactorClientHttpConnector aiClientHttpConnector(ConnectionProvider aiConnectionProvider) {
		HttpClient httpClient = HttpClient.create(aiConnectionProvider)
										  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
										  .responseTimeout(Duration.ofSeconds(180)); // key setting for ReadTimeoutException

		return new ReactorClientHttpConnector(httpClient);
	}

	@Bean
	@Primary
	public WebClient.Builder webClientBuilder(ReactorClientHttpConnector aiClientHttpConnector) {
		return WebClient.builder().clientConnector(aiClientHttpConnector);
	}
}