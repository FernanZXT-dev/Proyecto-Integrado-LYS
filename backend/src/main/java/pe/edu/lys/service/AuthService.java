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
  private final TokenService tokens;

  public AuthService(
      ClienteRepository clientes,
      UsuarioRepository usuarios,
      PasswordEncoder encoder,
      TokenService tokens) {
    this.clientes = clientes;
    this.usuarios = usuarios;
    this.encoder = encoder;
    this.tokens = tokens;
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

  @Transactional(readOnly = true)
  public LoginResponse loginCliente(LoginRequest datos) {
    Cliente c = clientes.findByCorreo(normalizar(datos.correo())).orElseThrow(this::credenciales);
    if (!c.isActivo() || !encoder.matches(datos.clave(), c.getClave())) throw credenciales();
    return respuesta("cliente:" + c.getId(), "CLIENTE");
  }

  @Transactional(readOnly = true)
  public LoginResponse loginPersonal(LoginRequest datos) {
    var u = usuarios.findByCorreo(normalizar(datos.correo())).orElseThrow(this::credenciales);
    if (!u.isActivo() || !encoder.matches(datos.clave(), u.getClave())) throw credenciales();
    return respuesta("usuario:" + u.getId(), u.getRol().getNombre());
  }

  private LoginResponse respuesta(String sujeto, String rol) {
    return new LoginResponse(tokens.generar(sujeto, rol), "Bearer", 3600, rol);
  }

  private ApiException credenciales() {
    return new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
  }

  public static String normalizar(String correo) {
    return correo.trim().toLowerCase(Locale.ROOT);
  }
}
