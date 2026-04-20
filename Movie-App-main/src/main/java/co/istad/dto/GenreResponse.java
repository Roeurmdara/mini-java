package co.istad.dto;

import co.istad.model.Genre;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GenreResponse {
    private List<Genre> genres;
}
