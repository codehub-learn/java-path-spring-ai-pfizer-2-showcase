package gr.codelearn.spring.ai.food.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gr.codelearn.spring.ai.AIConfig.KB_ID;

@RequiredArgsConstructor
@Slf4j
@Service
public class RagEtlService {
	private static final Pattern HEADING = Pattern.compile("(?m)^(#{1,6})\\s+(.*)\\s*$");

	private static final String MD_GLOB = "classpath*:rag/docs/*.md";
	private static final String PDF_GLOB = "classpath*:rag/docs/*.pdf";

	private final VectorStore vectorStore;
	private final TextSplitter tokenSplitter;

	public EtlResult reindex() throws Exception {
		var start = Instant.now();

		// TRUE re-index: delete old vectors for this KB before adding new ones.
		int deleted = deleteExistingKb();

		List<Document> raw = new ArrayList<>();
		raw.addAll(loadMarkdownDocs(MD_GLOB));
		raw.addAll(loadPdfDocs(PDF_GLOB));

		List<Document> sections = new ArrayList<>();
		for (Document d : raw) {
			String contentType = String.valueOf(d.getMetadata().getOrDefault("contentType", ""));
			if ("text/markdown".equals(contentType)) {
				sections.addAll(splitByMarkdownHeadings(d));
			} else {
				sections.add(d);
			}
		}

		List<Document> chunks = tokenSplitter.apply(sections);
		chunks = addChunkSequence(chunks);

		// Note: this "adds". If you want true reindex you typically delete existing KB rows first.
		vectorStore.add(chunks);

		var took = Duration.between(start, Instant.now());
		var result = new EtlResult(raw.size(), sections.size(), chunks.size(), deleted, took);

		log.info("ETL reindex done: {}.", result);
		return result;
	}

	public List<Document> search(final String query, final int topK) {
		return vectorStore.similaritySearch(
				SearchRequest.builder()
							 .query(query)
							 .topK(topK)
							 .filterExpression("kb == '" + KB_ID + "'")
							 .build()
										   );
	}

	private List<Document> loadMarkdownDocs(final String pattern) throws Exception {
		Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);

		List<Document> docs = new ArrayList<>();
		for (var r : resources) {
			String content = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

			Map<String, Object> meta = new HashMap<>();
			meta.put("kb", KB_ID);
			meta.put("source", safeFilename(r));
			meta.put("contentType", "text/markdown");

			docs.add(new Document(content, meta));
		}

		log.debug("Loaded {} markdown docs from {}.", docs.size(), pattern);
		return docs;
	}

	private List<Document> loadPdfDocs(final String pattern) throws Exception {
		Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);

		List<Document> docs = new ArrayList<>();
		for (var r : resources) {
			var reader = new PagePdfDocumentReader(r);
			List<Document> pdfPages = reader.get();

			for (var d : pdfPages) {
				Map<String, Object> meta = new HashMap<>(d.getMetadata());
				meta.put("kb", KB_ID);
				meta.put("source", safeFilename(r));
				meta.put("contentType", "application/pdf");

				docs.add(new Document(d.getText(), meta));
			}
		}

		log.debug("Loaded {} pdf paragraph docs from {}", docs.size(), pattern);
		return docs;
	}

	private List<Document> splitByMarkdownHeadings(final Document doc) {
		String text = doc.getText();
		Matcher m = HEADING.matcher(text);

		List<Integer> starts = new ArrayList<>();
		List<String> titles = new ArrayList<>();

		while (m.find()) {
			starts.add(m.start());
			titles.add(m.group(2));
		}

		if (starts.isEmpty()) {
			return List.of(doc);
		}

		List<Document> out = new ArrayList<>();
		for (int i = 0; i < starts.size(); i++) {
			int start = starts.get(i);
			int end = (i + 1 < starts.size()) ? starts.get(i + 1) : text.length();

			String sectionText = text.substring(start, end).trim();
			String sectionTitle = titles.get(i);

			Map<String, Object> meta = new HashMap<>(doc.getMetadata());
			meta.put("section", sectionTitle);

			out.add(new Document(sectionText, meta));
		}
		return out;
	}

	private List<Document> addChunkSequence(final List<Document> docs) {
		Map<String, Integer> counters = new HashMap<>();
		List<Document> out = new ArrayList<>(docs.size());

		for (Document d : docs) {
			String source = String.valueOf(d.getMetadata().getOrDefault("source", "unknown"));
			int n = counters.merge(source, 1, Integer::sum);

			Map<String, Object> meta = new HashMap<>(d.getMetadata());
			meta.put("chunk", n);

			out.add(new Document(d.getText(), meta));
		}
		return out;
	}

	private int deleteExistingKb() {
		// Delete all vectors whose metadata contains kb == KB_ID
		Filter.Expression deleteFilter = new Filter.Expression(
				Filter.ExpressionType.EQ,
				new Filter.Key("kb"),
				new Filter.Value(KB_ID)
		);

		// Some VectorStore implementations return boolean/void for delete.
		// We'll log intent and return -1 if we can't count deleted rows.
		try {
			vectorStore.delete(deleteFilter);
			log.info("Deleted existing vectors for kb='{}'.", KB_ID);
			return -1;
		} catch (Exception e) {
			log.warn("Failed to delete existing vectors for kb='{}': {}", KB_ID, e.getMessage(), e);
			return 0;
		}
	}

	private String safeFilename(final Resource r) {
		String name = Objects.requireNonNullElse(r.getFilename(), "unknown");
		return name.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	public record EtlResult(int rawDocs, int sections, int chunks, int deleted, Duration took) {
	}
}