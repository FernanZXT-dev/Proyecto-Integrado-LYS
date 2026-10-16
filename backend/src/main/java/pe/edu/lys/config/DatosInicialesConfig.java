package pe.edu.lys.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.lys.entity.*;
import pe.edu.lys.repository.*;
import pe.edu.lys.service.AuthService;

@Configuration
public class DatosInicialesConfig {
  @Bean
  CommandLineRunner datosIniciales(
      RolRepository roles,
      UsuarioRepository usuarios,
      PasswordEncoder encoder,
      @Value("${app.admin.correo:}") String correo,
      @Value("${app.admin.clave:}") String clave) {
    return args -> {
      for (String nombre : new String[] {"ADMINISTRADOR", "COCINA", "VENTAS", "REPARTO", "MESERA"}) {
        if (roles.findByNombre(nombre).isEmpty()) {
          Rol rol = new Rol();
          rol.setNombre(nombre);
          roles.save(rol);
        }
      }
      if (correo.isBlank() && clave.isBlank()) return;
      if (correo.isBlank() || clave.length() < 8 || clave.length() > 64)
        throw new IllegalArgumentException(
            "Configurar ADMIN_CORREO y ADMIN_CLAVE (8 a 64 caracteres)");
      String normalizado = AuthService.normalizar(correo);
      if (!usuarios.existsByCorreo(normalizado)) {
        Usuario u = new Usuario();
        u.setNombre("Administrador");
        u.setCorreo(normalizado);
        u.setClave(encoder.encode(clave));
        u.setRol(roles.findByNombre("ADMINISTRADOR").orElseThrow());
        usuarios.save(u);
      }
    };
  }
}
