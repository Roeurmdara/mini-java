package co.istad.model;

import co.istad.dto.TrailerResponse;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Movie {
    private String id;
    private String title;
    private Double popularity;
    private String release_date;
    private Double vote_average;
    private Trailer trailer;
}
