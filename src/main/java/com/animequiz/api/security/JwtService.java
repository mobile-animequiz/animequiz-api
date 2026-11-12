package com.animequiz.api.security;

import com.animequiz.api.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final long expirationSeconds;
    private final String issuer;

    public JwtService(JwtEncoder encoder,
                      @Value("${security.jwt.expiration-seconds:3600}") long expirationSeconds,
                      @Value("${security.jwt.issuer:animequiz-api}") String issuer) {
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("security.jwt.expiration-seconds deve ser maior que zero");
        }
        this.encoder = encoder;
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole()))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public long getExpirationSeconds() { return expirationSeconds; }
}
