package mx.com.gestishop.core.dto;

import lombok.Getter;
import lombok.Setter;
import mx.com.gestishop.core.enums.SearchOperation;

@Getter
@Setter
public class SearchFilterDTO {

    private String field;
    private Object value;
    private SearchOperation operation;
}
