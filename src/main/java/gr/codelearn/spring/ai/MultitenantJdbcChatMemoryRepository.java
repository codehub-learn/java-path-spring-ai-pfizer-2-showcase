package gr.codelearn.spring.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Repository
@Primary
public class MultitenantJdbcChatMemoryRepository implements ChatMemoryRepository {
	private final JdbcClient jdbcClient;

	@Override
	public List<String> findConversationIds() {
		return jdbcClient.sql("""
							  SELECT DISTINCT tenant_id, user_id, conversation_id
							  FROM  chat_memory
							  ORDER BY tenant_id, user_id, conversation_id
							  """)
						 .query((rs, rowNum) -> new Key(
								 rs.getString("tenant_id"),
								 rs.getString("user_id"),
								 rs.getString("conversation_id")
						 ).toString())
						 .list();
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Objects.requireNonNull(conversationId, "conversationId");

		var key = parseKey(conversationId);

		return jdbcClient.sql("""
							  SELECT role, content
							  FROM  chat_memory
							  WHERE tenant_id = :tenantId
							    AND user_id = :userId
							    AND conversation_id = :conversationId
							  ORDER BY message_index
							  """)
						 .param("tenantId", key.tenantId())
						 .param("userId", key.userId())
						 .param("conversationId", key.conversationId())
						 .query(messageRowMapper())
						 .list();

	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		if (CollectionUtils.isEmpty(messages)) {
			return;
		}
		Objects.requireNonNull(conversationId, "conversationId");

		var key = parseKey(conversationId);
		deleteInternal(key);

		var baseIndex = jdbcClient.sql("""
									   SELECT COALESCE(MAX(message_index), -1) + 1
									   FROM chat_memory
									   WHERE tenant_id = :tenantId
									     AND user_id = :userId
									     AND conversation_id = :conversationId
									   """)
								  .param("tenantId", key.tenantId())
								  .param("userId", key.userId())
								  .param("conversationId", key.conversationId())
								  .query(Integer.class)
								  .single();
		int base = (baseIndex == null ? 0 : baseIndex);
		
		String insertSql = """
						   INSERT INTO chat_memory (tenant_id, user_id, conversation_id, message_index, role, content)
						   VALUES (:tenantId, :userId, :conversationId, :messageIndex, :role, :content)
						   """;
		for (int i = 0; i < messages.size(); i++) {
			Message message = messages.get(i);

			Map<String, Object> params = new HashMap<>();
			params.put("tenantId", key.tenantId());
			params.put("userId", key.userId());
			params.put("conversationId", key.conversationId());
			params.put("messageIndex", base + i);   // contiguous, no hopping
			params.put("role", roleOf(message));
			params.put("content", message.getText());

			jdbcClient.sql(insertSql).params(params).update();
		}

	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Objects.requireNonNull(conversationId, "conversationId");
		deleteInternal(parseKey(conversationId));
	}

	private void deleteInternal(Key key) {
		jdbcClient.sql("""
					   DELETE FROM chat_memory
					   WHERE tenant_id = :tenantId
					     AND user_id = :userId
					     AND conversation_id = :conversationId
					   """)
				  .param("tenantId", key.tenantId())
				  .param("userId", key.userId())
				  .param("conversationId", key.conversationId())
				  .update();
	}

	private static Key parseKey(String compositeConversationId) {
		// Key.parse(...) is package-private in Key, so we parse here and still return a Key.
		String[] parts = compositeConversationId.split(":");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid conversation key");
		}
		return new Key(parts[0], parts[1], parts[2]);
	}

	private static String roleOf(Message message) {
		if (message instanceof UserMessage) {
			return "user";
		}
		if (message instanceof AssistantMessage) {
			return "assistant";
		}
		if (message instanceof SystemMessage) {
			return "system";
		}
		return "unknown";
	}

	private static RowMapper<Message> messageRowMapper() {
		return (rs, rowNum) -> {
			String role = rs.getString("role");
			String content = rs.getString("content");

			if ("user".equalsIgnoreCase(role)) {
				return new UserMessage(content);
			}
			if ("assistant".equalsIgnoreCase(role)) {
				return new AssistantMessage(content);
			}
			if ("system".equalsIgnoreCase(role)) {
				return new SystemMessage(content);
			}

			// Fallback: keep data visible even if role is null/unknown
			return new UserMessage(content);
		};
	}
}