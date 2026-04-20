package co.istad.dto;

import co.istad.model.Trailer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class TrailerResponse {
    List<Trailer> results;
}
