package gr.codelearn.spring.ai.movies;

import java.util.List;

public record MovieRecommendation(String title, int year, String genre, String posterUrl, String plot, String director,
								  List<String> actors) {
}