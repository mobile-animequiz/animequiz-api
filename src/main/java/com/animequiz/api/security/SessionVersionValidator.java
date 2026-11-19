package com.animequiz.api.security;
import com.animequiz.api.repository.UserRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
public final class SessionVersionValidator implements OAuth2TokenValidator<Jwt> {
    private final UserRepository users;
    public SessionVersionValidator(UserRepository users) { this.users=users; }
    public OAuth2TokenValidatorResult validate(Jwt token) {
        try {
            Object claim=token.getClaim("ver");
            int version=claim instanceof Number number ? number.intValue() : 0;
            var user=users.findById(Long.valueOf(token.getSubject()));
            if(user.isPresent() && user.get().getCredentialVersion()==version)
                return OAuth2TokenValidatorResult.success();
        } catch(RuntimeException ignored) { }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Sessão revogada ou usuário inexistente",null));
    }
}

