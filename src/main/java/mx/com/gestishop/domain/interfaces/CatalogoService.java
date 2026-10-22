package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.core.dto.CatalogoRefDTO;

import java.util.List;

public interface CatalogoService {

    List<CatalogoRefDTO> generos();

    List<CatalogoRefDTO> estadosCiviles();

    List<CatalogoRefDTO> municipios();
}
