package pe.edu.lys.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
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
  SecretKeySpec jwtKey(@Value("${app.jwt.secret}") String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32)
      throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes");
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKeySpec key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKeySpec key) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("lys-backend"));
    return decoder;
  }

  @Bean
  SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("rol");
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/registro",
                        "/api/auth/login/cliente",
                        "/api/auth/login/personal")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/productos",
                        "/api/productos/*",
                        "/api/categorias",
                        "/api/categorias/*",
                        "/api/catalogo",
                        "/api/combos",
                        "/api/combos/*",
                        "/api/promociones",
                        "/api/promociones/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/pedidos/*/iniciar-preparacion")
                    .hasAnyRole("ADMINISTRADOR", "COCINA")
                    .requestMatchers("/api/pedidos", "/api/pedidos/**")
                    .hasAnyRole("ADMINISTRADOR", "CLIENTE", "VENTAS", "MESERA")
                    .requestMatchers(
                        "/api/admin/**",
                        "/api/usuarios/**",
                        "/api/roles/**",
                        "/api/productos/**",
                        "/api/categorias/**",
                        "/api/combos/**",
                        "/api/promociones/**")
                    .hasRole("ADMINISTRADOR")
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
        .build();
  }
}
