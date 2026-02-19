package gr.codelearn.spring.ai.food.rag;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagChunkingConfig {
	@Bean
	public TextSplitter tokenSplitter() {
		// Spring AI 2.0.0-M2 requires punctuationMarks as List<Character>.
		// These help the splitter choose better breakpoints when chunking long text.
		List<Character> punctuationMarks = List.of('.', '!', '?', '\n');

		return new TokenTextSplitter(
				450,   // chunkSize (tokens target)
				280,   // minChunkSizeChars
				8,     // minChunkLengthToEmbed
				2000,  // maxNumChunks
				true,  // keepSeparator
				punctuationMarks
		);
	}
}