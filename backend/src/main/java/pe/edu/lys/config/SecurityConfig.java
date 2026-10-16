package pe.edu.lys.config;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import pe.edu.lys.exception.ApiException;
@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder() {
      @Override
      public String encode(CharSequence clave) {
        // BCrypt limita la entrada por bytes, no por cantidad de caracteres.
        if (clave.toString().getBytes(StandardCharsets.UTF_8).length > 72)
          throw new ApiException(
              HttpStatus.BAD_REQUEST, "La contraseña no puede superar 72 bytes UTF-8");
        return super.encode(clave);
      }
    };
  }

  @Bean
  SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/registro").permitAll().anyRequest().denyAll())
      .build();
  }
}
