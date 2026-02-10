package gr.codelearn.spring.ai.movies;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class MovieService {
	@Qualifier("moviesChatClient")
	private final ChatClient chatClient;

	public String getMovieRecommendationsAsString(final String genre, final int count) {
		String template = """
						  Generate a list of movies for the given {genre}.
						  Constraints:
						  - Provide exactly {count} bullet points
						  - Sort them in a descending order by year
						  """;
		return chatClient
				.prompt()
				.user(u -> u.text(template).params(Map.of("genre", genre, "count", count)))
				.call()
				.content();
	}

	public List<String> getMovieRecommendationsAsList(final String genre, final int count) {
		String template = """
						  Generate a list of movies for the given {genre}.
						  Constraints:
						  - Provide exactly {count} movies
						  - Sort them in a descending order by year
						  """;
		return chatClient
				.prompt()
				.user(u -> u.text(template).params(Map.of("genre", genre, "count", count)))
				.call()
				//.entity(List.class);// raw
				.entity(new ListOutputConverter(new DefaultConversionService()));
	}

	public List<MovieRecommendation> getMovieRecommendationsAsObject(final String genre, final int count) {
		String template = """
						  Generate a list of movies for the given {genre}.
						  Constraints:
						  - Provide exactly {count} movies
						  - Sort them in a descending order by year
						  """;
		return chatClient
				.prompt()
				.user(u -> u.text(template).params(Map.of("genre", genre, "count", count)))
				.call()
				.entity(new ParameterizedTypeReference<List<MovieRecommendation>>() {
				});
	}
}