package com.animequiz.api.configuration;

import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.repository.UserRepository;
import com.animequiz.api.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Never active in production, including a process that accidentally combines profiles. */
@Component
@Profile("!prod & (local | dev)")
@ConditionalOnProperty(name = "app.dev-admin.enabled", havingValue = "true")
public class LocalAdminInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder passwords;
    private final String password;

    public LocalAdminInitializer(UserRepository users, UserProfileRepository profiles, PasswordEncoder passwords,
            @Value("${app.dev-admin.password}") String password) {
        this.users = users; this.profiles = profiles; this.passwords = passwords; this.password = password;
    }

    @Override @Transactional
    public void run(ApplicationArguments ignored) {
        // Do not reset passwords or silently promote an existing player on restart.
        if (users.existsByEmailIgnoreCase("isaac@gmail.com")) return;
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("DEV_ADMIN_PASSWORD must contain 8 to 72 characters");
        }
        if (users.existsByUsernameIgnoreCase("isaac")) {
            throw new IllegalStateException("The local admin name is already in use; configure a fresh local database");
        }
        User admin = new User("isaac", "isaac@gmail.com");
        admin.setPasswordHash(passwords.encode(password));
        admin.setRole("ADMIN");
        users.save(admin);
        profiles.save(new UserProfile(admin, null, null));
    }
}
