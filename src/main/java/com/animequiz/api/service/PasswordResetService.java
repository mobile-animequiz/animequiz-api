package com.animequiz.api.service;
import com.animequiz.api.entity.*;
import com.animequiz.api.repository.*;
import com.animequiz.api.exception.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final PasswordMailer mailer;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final String publicOrigin;
    private final SecureRandom random=new SecureRandom();
    private record Bucket(long window,int count) {}
    private final ConcurrentHashMap<String,Bucket> budget=new ConcurrentHashMap<>();
    public PasswordResetService(UserRepository users,PasswordResetTokenRepository tokens,PasswordEncoder encoder,
        PasswordMailer mailer,ApplicationEventPublisher events,Clock clock,@Value("${app.public-origin:}") String publicOrigin) {
        this.users=users; this.tokens=tokens; this.encoder=encoder; this.mailer=mailer;
        this.events=events; this.clock=clock; this.publicOrigin=publicOrigin;
    }
    @Transactional
    public void request(String rawEmail) {
        URI base;
        try {
            base=URI.create(publicOrigin);
            if(!"https".equals(base.getScheme()) || base.getHost()==null || base.getUserInfo()!=null
                || base.getQuery()!=null || base.getFragment()!=null || !Set.of("","/").contains(base.getPath()))
                throw new IllegalArgumentException();
        } catch(Exception e) { throw new IntegrationUnavailableException("Recuperação de senha indisponível no momento"); }
        if(!mailer.isConfigured()) throw new IntegrationUnavailableException("Recuperação de senha indisponível no momento");
        String email=rawEmail.strip().toLowerCase(Locale.ROOT);
        long window=clock.instant().getEpochSecond()/3600;
        if(budget.size()>=10000) {
            budget.entrySet().removeIf(entry -> entry.getValue().window()<window);
            if(budget.size()>=10000 && !budget.containsKey(hash(email))) return;
        }
        Bucket value=budget.compute(hash(email),(key,old) -> old==null || old.window()!=window
            ? new Bucket(window,1) : new Bucket(window,old.count()+1));
        if(budget.size()>10000) budget.entrySet().removeIf(entry -> entry.getValue().window()<window);
        if(value.count()>3) return;
        var found=users.findByEmailIgnoreCase(email);
        if(found.isEmpty()) return;
        User user=users.findForUpdate(found.get().getId()).orElseThrow();
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new PasswordResetToken(user.getId(),hash(raw),clock.instant().plusSeconds(900)));
        events.publishEvent(new ResetMail(user.getEmail(),base.resolve("/reset-password.html").toString()+"#token="+raw));
    }
    @Transactional
    public void reset(String raw,String password) {
        if(raw==null || !raw.matches("[A-Za-z0-9_-]{43}") || password==null || password.length()<8
            || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new BadRequestException("Link inválido/expirado ou senha fora dos limites");
        String hash=hash(raw);
        Long id=tokens.findOwnerByHash(hash).orElseThrow(this::invalidLink);
        User user=users.findForUpdate(id).orElseThrow(this::invalidLink);
        PasswordResetToken token=tokens.findById(id).orElseThrow(this::invalidLink);
        if(!MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII),token.getTokenHash().getBytes(StandardCharsets.US_ASCII))
            || !token.getExpiresAt().isAfter(clock.instant())) throw invalidLink();
        user.setPasswordHash(encoder.encode(password)); user.revokeSessions(); users.save(user);
        tokens.delete(token);
    }
    private BadRequestException invalidLink() { return new BadRequestException("Link inválido, expirado ou já utilizado. Solicite um novo link."); }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public record ResetMail(String recipient,String url) {
        @Override public String toString() { return "ResetMail[redacted]"; }
    }
}

