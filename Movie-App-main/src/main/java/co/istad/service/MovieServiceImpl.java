package co.istad.service;

import co.istad.dto.*;
import co.istad.model.Genre;
import co.istad.model.Movie;
import co.istad.model.Trailer;
import co.istad.utils.API;
import tools.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MovieServiceImpl implements MovieService{
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper mapper = new ObjectMapper();
    //Search movies by title.
    @Override
    public MovieResponse getMoviesByTitleFromServer(Integer page,String title)  {
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String URL = String.format(
                "%s/search/movie?api_key=%s&query=%s&page=%d",
                API.BASE_URL,API.API_KEY,encodedTitle,page
        );
        MovieResponse allMovie = getFromApi(URL,MovieResponse.class);
        attachTrailers(allMovie);
        return allMovie;
    }
    // Get movie details using movie ID.
    @Override
    public MovieDetailResponse getMovieDetailsByTitleFromServer(String movieId) {
        String encodedString = URLEncoder.encode(movieId, StandardCharsets.UTF_8);
        String URL = String.format( "%s/movie/%s",  API.BASE_URL,encodedString );
        return getFromApi(URL,MovieDetailResponse.class);
    }
    //Get popular movies.
    @Override
    public MovieResponse getPopularMovies(Integer page) {
        String URL = String.format( "%s/movie/popular?lanuage=en-US&page=%d", API.BASE_URL,page);
        if(page > 500){ return null; }
        MovieResponse movieResponse =  getFromApi(URL,MovieResponse.class);
        attachTrailers(movieResponse);
        return movieResponse;
    }
    //Get all genres.
    @Override
    public List<Genre> getGenres() {
        String URL = String.format("%s/genre/movie/list", API.BASE_URL);
        GenreResponse genreResponse = getFromApi(URL,GenreResponse.class);
        return genreResponse.getGenres();
    }
    //Get movies by genre.
    @Override
    public MovieResponse getMoviesByGenre(Integer page, String genreId) {
        String encodedString = URLEncoder.encode(genreId, StandardCharsets.UTF_8);
        String URL = String.format( "%s/discover/movie?with_genres=%s&page=%d", API.BASE_URL, encodedString, page );
        MovieResponse movieResponse = getFromApi(URL,MovieResponse.class);
        attachTrailers(movieResponse);
        return movieResponse;
    }
    //Get movie cast by movie ID.
    @Override
    public CasterResponse getMovieCaster(String movieId) {
        String encodedString = URLEncoder.encode(movieId, StandardCharsets.UTF_8);
        String URL = String.format( "%s/movie/%s/credits", API.BASE_URL,encodedString );
        return getFromApi(URL,CasterResponse.class);
    }
    private <T> T getFromApi(String url, Class<T> clazz) {
        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .headers("Authorization", "Bearer " + API.BEARER_TOKEN,
                        "Accept", "application/json")
                .uri(URI.create(url)) .GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return mapper.readValue(response.body(), clazz);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Invalid ID!");
        }
    }
    //Get top rated movies.
    @Override
    public MovieResponse getTopRatedMovies(Integer page) {
        if (page > 500) { return null; }
        String URL = String.format("%s/movie/top_rated?language=en-US&page=%d", API.BASE_URL, page);
        MovieResponse movieResponse = getFromApi(URL, MovieResponse.class);
        attachTrailers(movieResponse);
        return movieResponse;
    }
    // Get upcoming movies.
    @Override
    public MovieResponse getUpcomingMovies(Integer page) {
        if (page > 500) { return null; }
        String URL = String.format("%s/movie/upcoming?language=en-US&page=%d", API.BASE_URL, page);
        MovieResponse movieResponse = getFromApi(URL, MovieResponse.class);
        attachTrailers(movieResponse);
        return movieResponse;
    }
    // Add trailer information into each movie.
    private void attachTrailers(MovieResponse movieResponse) {
        for (Movie movie : movieResponse.getResults()) {
            String url = String.format( "%s/movie/%s/videos", API.BASE_URL, movie.getId() );
            TrailerResponse trailerData = getFromApi(url, TrailerResponse.class);
            if (trailerData.getResults() != null) {
                for (Trailer t : trailerData.getResults()) {
                    if ("Trailer".equals(t.getType())) {
                        movie.setTrailer(t);
                        break;
                    }
                }
            }
        }
    }
}
