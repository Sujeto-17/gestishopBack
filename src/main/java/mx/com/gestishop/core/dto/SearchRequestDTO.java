package mx.com.gestishop.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchRequestDTO {
    private List<SearchFilterDTO> filters;
    private Integer page = 0;
    private Integer size = 10;
    private String shortField;
    private String shortDirection = "ASC";
}
