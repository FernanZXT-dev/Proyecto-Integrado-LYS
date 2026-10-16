package pe.edu.lys.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.lys.dto.AuthDtos.CuentaResponse;
import pe.edu.lys.dto.UsuarioDtos.*;
import pe.edu.lys.entity.Usuario;
import pe.edu.lys.exception.ApiException;
import pe.edu.lys.repository.*;

@Service
public class UsuarioService {
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final PasswordEncoder encoder;

  public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder) {
    this.usuarios = usuarios;
    this.roles = roles;
    this.encoder = encoder;
  }



  @Transactional(readOnly = true)
  public List<RolResponse> roles() {
    return roles.findAll().stream().map(r -> new RolResponse(r.getId(), r.getNombre())).toList();
  }

}
