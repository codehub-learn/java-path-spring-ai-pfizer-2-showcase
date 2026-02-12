package gr.codelearn.spring.ai;

import io.netty.channel.ChannelOption;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.sql.DataSource;
import java.time.Duration;

@Configuration
public class AIConfig {
	@Bean
	public ChatClient moviesChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
		return chatClientBuilder
				.defaultSystem("""
							   You are a helpful CLI assistant. You are expert in movies.
							   Keep answers concise.
							   When asked for a list of things, use a numbered list.
							   Always return the list of actors
							   """)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.build();
	}

	@Bean
	@Primary
	public ChatClient ChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
		return chatClientBuilder
				.defaultSystem("""
							   You are a helpful CLI assistant. You are expert in sports.
							   Keep answers concise.
							   When asked for a list of things, use a numbered list.
							   """)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.build();
	}

	@Bean
	public ChatMemory getChatMemory(ChatMemoryRepository chatMemoryRepository) {
		return MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).maxMessages(10).build();
	}

	//	We will use our own custom repository
	public ChatMemoryRepository chatMemoryRepository(DataSource dataSource) {
		var jdbcTemplate = new JdbcTemplate(dataSource);
		return JdbcChatMemoryRepository.builder()
									   .jdbcTemplate(jdbcTemplate)
									   .build();
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
	@Primary
	public WebClient.Builder webClientBuilder(ReactorClientHttpConnector aiClientHttpConnector) {
		return WebClient.builder().clientConnector(aiClientHttpConnector);
	}

	@Bean
	public ReactorClientHttpConnector aiClientHttpConnector(ConnectionProvider aiConnectionProvider) {
		HttpClient httpClient = HttpClient.create(aiConnectionProvider)
										  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
										  .responseTimeout(Duration.ofSeconds(180)); // key setting for ReadTimeoutException

		return new ReactorClientHttpConnector(httpClient);
	}
}