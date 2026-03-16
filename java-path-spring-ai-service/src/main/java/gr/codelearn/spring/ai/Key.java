package gr.codelearn.spring.ai;

public record Key(String tenantId, String userId, String conversationId) {
	static Key parse(String composite) {
		String[] parts = composite.split(":");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid conversation key");
		}
		return new Key(parts[0], parts[1], parts[2]);
	}

	public String toString() {
		return tenantId + ":" + userId + ":" + conversationId;
	}
}