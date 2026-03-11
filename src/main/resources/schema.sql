CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS public.vector_store
(
	id        UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
	content   TEXT,
	metadata  JSONB,
	embedding VECTOR(768)
);

CREATE INDEX IF NOT EXISTS idx_vector_store_metadata
	ON public.vector_store USING GIN (metadata);

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
	ON public.vector_store USING HNSW (embedding vector_cosine_ops);