package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.CambiarPasswordRequestDTO;
import mx.com.gestishop.application.dto.request.LoginRequestDTO;
import mx.com.gestishop.application.dto.response.LoginResponseDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO dto, String ip, String userAgent);

    LoginResponseDTO refrescarToken(String refreshTokenPlano);

    void logout(String refreshTokenPlano);

    void cambiarPassword(CambiarPasswordRequestDTO dto);
}
