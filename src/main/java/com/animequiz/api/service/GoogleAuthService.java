package com.animequiz.api.service;
import com.animequiz.api.dto.*;
import com.animequiz.api.entity.*;
import com.animequiz.api.repository.*;
import com.animequiz.api.security.JwtService;
import com.animequiz.api.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class GoogleAuthService {
    private final GoogleIdentityVerifier verifier;
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final JwtService jwt;
    public GoogleAuthService(GoogleIdentityVerifier verifier,UserRepository users,UserProfileRepository profiles,JwtService jwt) {
        this.verifier=verifier; this.users=users; this.profiles=profiles; this.jwt=jwt;
    }
    @Transactional
    public AuthResponse login(String idToken) {
        var identity=verifier.verify(idToken);
        var known=users.findByGoogleSubject(identity.subject());
        if(known.isPresent()) return response(known.get());
        if(users.existsByEmailIgnoreCase(identity.email()))
            throw new ConflictException("Entre com e-mail e senha e vincule o Google nas configurações da conta");
        User user=new User("jogador"+UUID.randomUUID().toString().replace("-","").substring(0,20),identity.email());
        user.setGoogleSubject(identity.subject()); user=users.save(user);
        profiles.save(new UserProfile(user,null,null)); return response(user);
    }
    @Transactional
    public void link(Long id,String idToken) {
        var identity=verifier.verify(idToken);
        User owner=users.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if(!owner.getEmail().equalsIgnoreCase(identity.email()))
            throw new ConflictException("Use a conta Google com o mesmo e-mail desta conta");
        var linked=users.findByGoogleSubject(identity.subject());
        if(linked.isPresent() && !linked.get().getId().equals(id))
            throw new ConflictException("Essa identidade Google já está vinculada");
        if(owner.getGoogleSubject()!=null && !owner.getGoogleSubject().equals(identity.subject()))
            throw new ConflictException("Outra identidade Google já está vinculada à conta");
        owner.setGoogleSubject(identity.subject()); users.save(owner);
    }
    private AuthResponse response(User user) {
        return new AuthResponse(jwt.generateToken(user),"Bearer",jwt.getExpirationSeconds(),
            new AuthUserResponse(user.getId(),user.getUsername(),user.getEmail(),user.getDateOfBirth()));
    }
}

