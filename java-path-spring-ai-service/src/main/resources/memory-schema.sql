CREATE TABLE IF NOT EXISTS public.chat_memory
(
	id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	tenant_id       VARCHAR(64) NOT NULL,
	user_id         VARCHAR(64) NOT NULL,
	conversation_id VARCHAR(64) NOT NULL,
	message_index   INT         NOT NULL,
	role            VARCHAR(20),
	content         TEXT,
	timestamp       TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_memory
	ON public.chat_memory (tenant_id, user_id, conversation_id, message_index);