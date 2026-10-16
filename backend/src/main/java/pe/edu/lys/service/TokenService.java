package pe.edu.lys.service;

import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private final JwtEncoder encoder;

  public TokenService(JwtEncoder encoder) {
    this.encoder = encoder;
  }

  public String generar(String sujeto, String rol) {
    Instant ahora = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("lys-backend")
            .subject(sujeto)
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(3600))
            .claim("rol", rol)
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
