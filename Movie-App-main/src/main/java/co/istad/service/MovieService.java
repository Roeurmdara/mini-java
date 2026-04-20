package co.istad.service;

import co.istad.dto.CasterResponse;
import co.istad.dto.MovieDetailResponse;
import co.istad.dto.MovieResponse;
import co.istad.model.Genre;
import co.istad.model.Movie;

import java.util.List;

public interface MovieService {
    MovieResponse getMoviesByTitleFromServer(Integer page,String title);
    MovieDetailResponse getMovieDetailsByTitleFromServer(String movieId);
    MovieResponse getPopularMovies(Integer page);
    List<Genre> getGenres();
    MovieResponse getMoviesByGenre(Integer page, String genreId);
    CasterResponse getMovieCaster(String movieId);
    MovieResponse getTopRatedMovies(Integer page);
    MovieResponse getUpcomingMovies(Integer page);
}
