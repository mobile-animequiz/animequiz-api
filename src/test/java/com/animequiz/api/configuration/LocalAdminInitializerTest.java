package com.animequiz.api.configuration;

import com.animequiz.api.entity.User;
import com.animequiz.api.repository.UserRepository;
import com.animequiz.api.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalAdminInitializerTest {
    @Test void createsHashedAdminOnceWithoutResettingExistingAccount() throws Exception {
        var users = mock(UserRepository.class);
        var profiles = mock(UserProfileRepository.class);
        var encoder = new BCryptPasswordEncoder(4);
        var seed = new LocalAdminInitializer(users, profiles, encoder, "local-test-password");
        when(users.existsByEmailIgnoreCase("isaac@gmail.com")).thenReturn(false, true);
        seed.run(null);
        seed.run(null);
        var captured = ArgumentCaptor.forClass(User.class);
        verify(users, times(1)).save(captured.capture());
        var admin = captured.getValue();
        assertEquals("isaac", admin.getUsername());
        assertEquals("isaac@gmail.com", admin.getEmail());
        assertEquals("ADMIN", admin.getRole());
        assertNotEquals("local-test-password", admin.getPasswordHash());
        assertTrue(encoder.matches("local-test-password", admin.getPasswordHash()));
    }
    @Test void existingEmailIsNeverPromotedOrGivenANewPassword() {
        var users = mock(UserRepository.class);
        var profiles = mock(UserProfileRepository.class);
        when(users.existsByEmailIgnoreCase("isaac@gmail.com")).thenReturn(true);
        new LocalAdminInitializer(users, profiles, new BCryptPasswordEncoder(4), "ignored-test-password").run(null);
        verify(users, never()).save(any());
        verifyNoInteractions(profiles);
    }
    @Test void productionCannotActivateSeederEvenWithLocalProfileAlsoEnabled() {
        var expression = LocalAdminInitializer.class.getAnnotation(Profile.class).value()[0];
        assertTrue(Profiles.of(expression).matches(Set.of("local")::contains));
        assertTrue(Profiles.of(expression).matches(Set.of("dev")::contains));
        assertFalse(Profiles.of(expression).matches(Set.of("prod", "local")::contains));
        assertFalse(Profiles.of(expression).matches(Set.of("default")::contains));
    }
}
