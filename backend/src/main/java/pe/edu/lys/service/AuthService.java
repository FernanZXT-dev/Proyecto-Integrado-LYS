package pe.edu.lys.service;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.lys.dto.AuthDtos.*;
import pe.edu.lys.entity.Cliente;
import pe.edu.lys.exception.ApiException;
import pe.edu.lys.repository.*;

@Service
public class AuthService {
  private final ClienteRepository clientes;
  private final UsuarioRepository usuarios;
  private final PasswordEncoder encoder;

  public AuthService(
      ClienteRepository clientes,
      UsuarioRepository usuarios,
      PasswordEncoder encoder) {
    this.clientes = clientes;
    this.usuarios = usuarios;
    this.encoder = encoder;
  }

  @Transactional
  public CuentaResponse registrar(RegistroClienteRequest datos) {
    String correo = normalizar(datos.correo());
    if (clientes.existsByCorreo(correo))
      throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
    Cliente cliente = new Cliente();
    cliente.setNombre(datos.nombre().trim());
    cliente.setCorreo(correo);
    cliente.setClave(encoder.encode(datos.clave()));
    cliente.setTelefono(datos.telefono());
    clientes.save(cliente);
    return new CuentaResponse(
        cliente.getId(), cliente.getNombre(), cliente.getCorreo(), "CLIENTE", cliente.isActivo());
  }





  public static String normalizar(String correo) {
    return correo.trim().toLowerCase(Locale.ROOT);
  }
}
