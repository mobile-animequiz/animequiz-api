package com.animequiz.api.configuration;

import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.repository.UserRepository;
import com.animequiz.api.repository.UserProfileRepository;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit operator provisioning, including production. No public password or HTTP endpoint. */
@Component
@Order(-100)
@ConditionalOnProperty(name="app.admin-bootstrap.enabled", havingValue="true")
public class AdminBootstrapInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder passwords;
    private final Validator validator;
    private final String email;
    private final String username;
    private final String password;
    private final boolean updateExisting;

    public AdminBootstrapInitializer(UserRepository users, UserProfileRepository profiles,
            PasswordEncoder passwords, Validator validator,
            @Value("${app.admin-bootstrap.email:isaac@gmail.com}") String email,
            @Value("${app.admin-bootstrap.username:isaac}") String username,
            @Value("${app.admin-bootstrap.password:}") String password,
            @Value("${app.admin-bootstrap.update-existing:false}") boolean updateExisting) {
        this.users=users; this.profiles=profiles; this.passwords=passwords; this.validator=validator;
        this.email=email.trim().toLowerCase(Locale.ROOT); this.username=username.trim();
        this.password=password; this.updateExisting=updateExisting;
    }

    @Override @Transactional
    public void run(ApplicationArguments ignored) {
        // Validate even on restart: configuration must never permit the public demo password.
        if (password.length()<16 || password.getBytes(StandardCharsets.UTF_8).length>72
                || password.isBlank() || password.chars().distinct().count()<5) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD requires 16 or more characters, 5 distinct characters and at most 72 UTF-8 bytes");
        }
        User candidate=new User(username,email);
        if (!validator.validate(candidate).isEmpty()) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_EMAIL or ADMIN_BOOTSTRAP_USERNAME is invalid");
        }
        User existing=users.findByEmailIgnoreCase(email).orElse(null);
        if (existing!=null) {
            boolean unchanged="ADMIN".equals(existing.getRole()) && existing.getPasswordHash()!=null
                    && passwords.matches(password,existing.getPasswordHash());
            if (unchanged) {
                log.info("Admin bootstrap: configured administrator already provisioned; no changes");
                return;
            }
            if (!updateExisting) {
                log.warn("Admin bootstrap: email already exists; account unchanged. Explicit ADMIN_BOOTSTRAP_UPDATE_EXISTING=true is required to promote/reset this account");
                return;
            }
            existing.setRole("ADMIN");
            existing.setPasswordHash(passwords.encode(password));
            existing.revokeSessions();
            users.save(existing);
            if (!profiles.existsById(existing.getId())) profiles.save(new UserProfile(existing,null,null));
            log.info("Admin bootstrap: explicitly updated configured account; previous sessions revoked");
            return;
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_USERNAME is already in use; choose a different administrator username");
        }
        candidate.setRole("ADMIN");
        candidate.setPasswordHash(passwords.encode(password));
        users.save(candidate);
        profiles.save(new UserProfile(candidate,null,null));
        log.info("Admin bootstrap: administrator created");
    }
}
