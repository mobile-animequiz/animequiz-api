package com.animequiz.api.service;
import com.animequiz.api.exception.IntegrationUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
@Service
public class GoogleIdentityVerifier {
    private final Set<String> audiences;
    private final JwtDecoder decoder;
    public GoogleIdentityVerifier(@Value("${google.client-ids:}") String configured) {
        audiences=new HashSet<>();
        for(String value:configured.split(",")) if(!value.isBlank()) audiences.add(value.strip());
        decoder=NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build();
    }
    public Identity verify(String rawToken) {
        if(audiences.isEmpty()) throw new IntegrationUnavailableException("Login com Google indisponível no momento");
        try { return validateClaims(decoder.decode(rawToken),audiences,Instant.now()); }
        catch(JwtException|IllegalArgumentException e) { throw new BadCredentialsException("Identidade Google inválida"); }
    }
    static Identity validateClaims(Jwt token,Set<String> audiences,Instant now) {
        String issuer=token.getClaimAsString("iss"), email=token.getClaimAsString("email"), subject=token.getSubject();
        if(issuer==null || !Set.of("accounts.google.com","https://accounts.google.com").contains(issuer)
            || token.getAudience()==null || Collections.disjoint(token.getAudience(),audiences)
            || token.getExpiresAt()==null || !token.getExpiresAt().isAfter(now)
            || !Boolean.TRUE.equals(token.getClaim("email_verified"))
            || subject==null || subject.isBlank() || subject.length()>255
            || email==null || email.length()>254 || !email.contains("@"))
            throw new BadCredentialsException("Identidade Google inválida");
        return new Identity(subject,email.strip().toLowerCase(Locale.ROOT));
    }
    public record Identity(String subject,String email) {}
}

