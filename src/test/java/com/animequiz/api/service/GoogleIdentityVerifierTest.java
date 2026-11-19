package com.animequiz.api.service;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.BadCredentialsException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class GoogleIdentityVerifierTest {
    private Jwt token(String issuer,String audience,boolean verified,Instant expiry) {
        return Jwt.withTokenValue("signed").header("alg","RS256").subject("subject-1")
            .claim("iss",issuer).audience(List.of(audience)).expiresAt(expiry)
            .claim("email","Player@Example.test").claim("email_verified",verified).build();
    }
    @Test void requiresTrustedIssuerAudienceVerifiedEmailAndFreshExpiration() {
        Instant now=Instant.now();
        assertEquals("player@example.test",GoogleIdentityVerifier.validateClaims(
            token("https://accounts.google.com","client",true,now.plusSeconds(100)),Set.of("client"),now).email());
        assertThrows(BadCredentialsException.class,()->GoogleIdentityVerifier.validateClaims(
            token("https://attacker.test","client",true,now.plusSeconds(100)),Set.of("client"),now));
        assertThrows(BadCredentialsException.class,()->GoogleIdentityVerifier.validateClaims(
            token("accounts.google.com","other-client",true,now.plusSeconds(100)),Set.of("client"),now));
        assertThrows(BadCredentialsException.class,()->GoogleIdentityVerifier.validateClaims(
            token("accounts.google.com","client",false,now.plusSeconds(100)),Set.of("client"),now));
        assertThrows(BadCredentialsException.class,()->GoogleIdentityVerifier.validateClaims(
            token("accounts.google.com","client",true,now.minusSeconds(1)),Set.of("client"),now));
    }
}

