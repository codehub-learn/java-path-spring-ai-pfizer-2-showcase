package gr.codelearn.spring.ai.movies;

import gr.codelearn.spring.ai.Key;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/{tenant}/{user}/movies", method = RequestMethod.GET)
public class MovieController {
	private final MovieService movieService;

	@GetMapping(params = {"genre", "type=STRING"})
	public String getMovieRecommendationsAsString(@PathVariable String tenant, @PathVariable String user,
												  @RequestParam final String genre,
												  @RequestParam(required = false, defaultValue = "10") final int count) {
		return movieService.getMovieRecommendationsAsString(genre, count, new Key(tenant, user, "movie_recommendations"));
	}

	@GetMapping(params = {"genre", "type=LIST"})
	public List<String> getMovieRecommendationsAsList(@PathVariable String tenant, @PathVariable String user,
													  @RequestParam final String genre,
													  @RequestParam(required = false, defaultValue = "10") final int count) {
		return movieService.getMovieRecommendationsAsList(genre, count, new Key(tenant, user, "movie_recommendations"));
	}

	@GetMapping(params = {"genre", "type=OBJECT"})
	public List<MovieRecommendation> getMovieRecommendationsAsObject(@PathVariable String tenant, @PathVariable String user,
																	 @RequestParam final String genre,
																	 @RequestParam(required = false, defaultValue = "10") final int count) {
		return movieService.getMovieRecommendationsAsObject(genre, count, new Key(tenant, user, "movie_recommendations"));
	}
}