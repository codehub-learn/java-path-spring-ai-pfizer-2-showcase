package gr.codelearn.spring.ai;

import gr.codelearn.spring.ai.food.catalog.StoreCatalogService;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
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

@RequiredArgsConstructor
@Configuration
public class AIConfig {
	// In case we inject VectorStore, there will be circular dependency issue.
	private final ObjectProvider<VectorStore> vectorStoreProvider;
	private final StoreCatalogService storeCatalogService;

	public static final String KB_ID = "quickbite-food-delivery";

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
				.defaultOptions(ChatOptions.builder()
										   .temperature(0.4)
										   .build())
				.build();
	}

	@Bean
	public ChatClient sportsChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
		return chatClientBuilder
				.defaultSystem("""
							   You are a helpful CLI assistant. You are expert in sports.
							   Keep answers concise and short, no more than 50 words unless necessary such as a list.
							   When asked for a list of things, use a numbered list.
							   """)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.build();
	}

	@Bean
	public ChatClient foodSupportChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
		return chatClientBuilder
				.defaultSystem("""
							   You are QuickBite Support Assistant.
							   You are expert in QuickBite food delivery support.
							   Answer ONLY from the provided knowledge base context.
							   Do NOT use tools.
							   If the answer is not in the context, say you don't know and ask a clarifying question.
							   Always cite the section/source from the context when possible.
							   Keep answers concise and actionable.
							   """)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
								 QuestionAnswerAdvisor.builder(vectorStoreProvider.getObject())
													  .searchRequest(SearchRequest.builder()
																				  // Query is provided at runtime from the user's message
																				  // by the advisor;
																				  // some versions still require a non-null query in the
																				  // object, so keep it empty.
																				  .query("")
																				  .similarityThreshold(0.3)
																				  // Add a filter expression to only retrieve documents
																				  // related to the QuickBite food delivery service.
																				  .topK(10)
																				  .filterExpression("kb == '" + KB_ID + "'")
																				  .build())
													  .build())
				.defaultOptions(ChatOptions.builder()
										   .temperature(0.3)
										   .build())
				.build();
	}

	@Bean
	public ChatClient foodCatalogChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
		return chatClientBuilder
				.defaultSystem("""
							   You are QuickBite Catalog Assistant.
							   Help users find stores and inspect menus registered on the QuickBite platform.
							   
							   You must use the available tools to retrieve store and menu data.
							   Never invent, assume, infer, or suggest stores that were not returned by a tool.
							   Only mention stores and menu items that exist in the QuickBite catalog database as returned by the tools.
							   If a tool returns no matching stores or menu items, clearly say that no matching results were found in the QuickBite catalog.
							   If the available tool result is incomplete, say so instead of guessing.
							   
							   Tool selection rules:
							   - Use find-stores-by-title when the user mentions a store name or part of it.
							   - Use find-stores-by-cuisine when the user asks for stores by cuisine such as sushi, pizza, italian, japanese, burgers, vegan, or desserts.
							   - Use find-stores-by-menu-item-category when the user asks for stores serving a menu category.
							   - Use find-stores-by-menu-item-name when the user asks for a specific dish or item name.
							   - Use get-store-menu when the user asks for the menu of a known store.
							   
							   Keep answers concise, structured, and grounded strictly in tool results.
							   """)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.defaultTools(storeCatalogService)
				.defaultOptions(ChatOptions.builder()
										   .temperature(0.1)
										   .build())
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