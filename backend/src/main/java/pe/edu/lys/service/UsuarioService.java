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

  @Transactional
  public CuentaResponse crear(CrearUsuarioRequest datos) {
    String correo = AuthService.normalizar(datos.correo());
    if (usuarios.existsByCorreo(correo))
      throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
    var rol =
        roles
            .findById(datos.rolId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    Usuario u = new Usuario();
    u.setNombre(datos.nombre().trim());
    u.setCorreo(correo);
    u.setClave(encoder.encode(datos.clave()));
    u.setRol(rol);
    return respuesta(usuarios.save(u));
  }

  @Transactional(readOnly = true)
  public List<CuentaResponse> listar() {
    return usuarios.findAll().stream().map(this::respuesta).toList();
  }

  @Transactional(readOnly = true)
  public List<RolResponse> roles() {
    return roles.findAll().stream().map(r -> new RolResponse(r.getId(), r.getNombre())).toList();
  }

  private CuentaResponse respuesta(Usuario u) {
    return new CuentaResponse(
        u.getId(), u.getNombre(), u.getCorreo(), u.getRol().getNombre(), u.isActivo());
  }
}
