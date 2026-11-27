package com.animequiz.api.configuration;

import com.animequiz.api.entity.User;
import com.animequiz.api.repository.UserRepository;
import com.animequiz.api.repository.UserProfileRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBootstrapInitializerTest {
    private static final ValidatorFactory factory=Validation.buildDefaultValidatorFactory();
    private final Validator validator=factory.getValidator();
    private final UserRepository users=mock(UserRepository.class);
    private final UserProfileRepository profiles=mock(UserProfileRepository.class);
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    private static final String PASSWORD="Bootstrap-test-only-passphrase9!";
    @AfterAll static void closeValidator() { factory.close(); }
    private AdminBootstrapInitializer seed(boolean update) {
        return new AdminBootstrapInitializer(users,profiles,encoder,validator," ISAAC@gmail.com ","isaac",PASSWORD,update);
    }
    @Test void disabledByDefaultWithoutAnySecretOrRepositoryBeans() {
        new ApplicationContextRunner().withUserConfiguration(AdminBootstrapInitializer.class)
            .run(context->{ assertNull(context.getStartupFailure()); assertFalse(context.containsBean("adminBootstrapInitializer")); });
    }
    @Test void createsHashedAdministratorAndProfileExactlyOnce() {
        seed(false).run(null);
        var capture=ArgumentCaptor.forClass(User.class);
        verify(users).save(capture.capture());
        User saved=capture.getValue(); saved.setId(1L);
        assertEquals("ADMIN",saved.getRole());
        assertEquals("isaac@gmail.com",saved.getEmail());
        assertEquals("isaac",saved.getUsername());
        assertNotEquals(PASSWORD,saved.getPasswordHash());
        assertTrue(encoder.matches(PASSWORD,saved.getPasswordHash()));
        verify(profiles).save(any());
        when(users.findByEmailIgnoreCase("isaac@gmail.com")).thenReturn(Optional.of(saved));
        seed(true).run(null);
        verify(users,times(1)).save(any());
        assertEquals(0,saved.getCredentialVersion());
    }
    @Test void neverPromotesOrResetsAnExistingAccountWithoutExplicitFlag() {
        User player=new User("custom-name","isaac@gmail.com"); player.setId(1L);
        player.setPasswordHash(encoder.encode("previous-test-password"));
        String previous=player.getPasswordHash();
        when(users.findByEmailIgnoreCase("isaac@gmail.com")).thenReturn(Optional.of(player));
        seed(false).run(null);
        assertEquals("USER",player.getRole()); assertEquals(previous,player.getPasswordHash());
        verify(users,never()).save(any()); verifyNoInteractions(profiles);
    }
    @Test void explicitExistingAccountUpdateRevokesSessionsAndPreservesProfile() {
        User player=new User("custom-name","isaac@gmail.com"); player.setId(1L);
        when(users.findByEmailIgnoreCase("isaac@gmail.com")).thenReturn(Optional.of(player));
        when(profiles.existsById(1L)).thenReturn(true);
        seed(true).run(null);
        assertEquals("ADMIN",player.getRole()); assertEquals("custom-name",player.getUsername());
        assertTrue(encoder.matches(PASSWORD,player.getPasswordHash()));
        assertEquals(1,player.getCredentialVersion());
        verify(profiles,never()).save(any());
    }
    @ParameterizedTest @ValueSource(strings={"", "123123123", "aaaaaaaaaaaaaaaa", "                "})
    void rejectsMissingOrWeakPasswordsWithoutWritingAccounts(String password) {
        var seed=new AdminBootstrapInitializer(users,profiles,encoder,validator,"isaac@gmail.com","isaac",password,false);
        assertThrows(IllegalStateException.class,()->seed.run(null));
        verifyNoInteractions(users,profiles);
    }
    @Test void rejectsMoreThan72Utf8Bytes() {
        String password="é".repeat(35)+"abcd";
        assertThrows(IllegalStateException.class,()->new AdminBootstrapInitializer(users,profiles,encoder,validator,
            "isaac@gmail.com","isaac",password,false).run(null));
        verifyNoInteractions(users,profiles);
    }
    @Test void rejectsInvalidEmailWithoutWritingAccounts() {
        assertThrows(IllegalStateException.class,()->new AdminBootstrapInitializer(users,profiles,encoder,validator,
            "invalid-email","isaac",PASSWORD,false).run(null));
        verifyNoInteractions(users,profiles);
    }
    @Test void usernameCollisionCannotTakeOverAnotherAccount() {
        when(users.existsByUsernameIgnoreCase("isaac")).thenReturn(true);
        assertThrows(IllegalStateException.class,()->seed(false).run(null));
        verify(users,never()).save(any()); verifyNoInteractions(profiles);
    }
}
