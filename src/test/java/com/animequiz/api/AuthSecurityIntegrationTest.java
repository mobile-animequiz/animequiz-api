package com.animequiz.api;

import com.animequiz.api.entity.User;
import com.animequiz.api.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.time.Instant;
import static org.hamcrest.Matchers.nullValue;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSecurityIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper mapper;
    @Autowired private org.springframework.context.ApplicationContext applicationContext;

    @Test
    void registerWorksWithoutDateOfBirthAndStoresOnlyPasswordHash() throws Exception {
        String response = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Conta Nova\",\"email\":\"nova@example.com\",\"password\":\"SenhaSegura123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.dateOfBirth").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).path("user").path("id").asLong();
        User saved = users.findById(id).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("SenhaSegura123");
        assertThat(passwordEncoder.matches("SenhaSegura123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getDateOfBirth()).isNull();
        assertThat(saved.getRole()).isEqualTo("USER");
    }

    @Test
    void registerAcceptsExplicitNullDateOfBirth() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Data Nula\",\"email\":\"nula@example.com\",\"password\":\"SenhaSegura123\",\"dateOfBirth\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.dateOfBirth").value(nullValue()));
    }

    @Test
    void registerStoresOptionalDateWhenProvided() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Com Data\",\"email\":\"data@example.com\",\"password\":\"SenhaSegura123\",\"dateOfBirth\":\"2005-04-20\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.dateOfBirth").value("2005-04-20"));
    }

    @Test
    void registerRejectsDuplicateEmailAndWeakPassword() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Conta Nova\",\"email\":\"duplicado@example.com\",\"password\":\"SenhaSegura123\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Outra Conta\",\"email\":\"DUPLICADO@example.com\",\"password\":\"SenhaSegura123\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Senha Curta\",\"email\":\"curta@example.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsJwtAndCurrentUserAcceptsIt() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"Login Teste\",\"email\":\"login@example.com\",\"password\":\"SenhaSegura123\"}"))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"LOGIN@example.com\",\"password\":\"SenhaSegura123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = mapper.readTree(login);
        String token = body.path("accessToken").asText();
        long id = body.path("user").path("id").asLong();
        assertThat(token).isNotBlank();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    void invalidCredentialsAndInvalidTokenAreRejected() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"naruto@konoha.com\",\"password\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/users/me").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredSignedJwtIsRejected() throws Exception {
        JwtEncoder encoder = applicationContext.getBean(JwtEncoder.class);
        Instant now = Instant.now();
        String expired = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), JwtClaimsSet.builder()
                .issuer("animequiz-api").subject("1").issuedAt(now.minusSeconds(720))
                .expiresAt(now.minusSeconds(600)).claim("roles", java.util.List.of("USER")).build())).getTokenValue();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedAttemptRequiresAuthenticationAndIgnoresClientUserId() throws Exception {
        String payload = "{\"userId\":2,\"answers\":[{\"questionId\":1,\"optionId\":1}]}";
        mvc.perform(post("/quizzes/1/attempts").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/quizzes/1/attempts").with(jwt().jwt(token -> token.subject("1").claim("roles", java.util.List.of("USER"))))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void usersCanReadOnlyTheirOwnDetailedProfileUnlessTheyAreAdmin() throws Exception {
        mvc.perform(get("/users/2/profile").with(jwt().jwt(token -> token.subject("1").claim("roles", java.util.List.of("USER")))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/users/2/profile").with(jwt().jwt(token -> token.subject("1").claim("roles", java.util.List.of("ADMIN")))))
                .andExpect(status().isOk());
    }

    @Test
    void jwtRoleClaimGrantsAdminAccessAfterAccountPromotion() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Admin Conta\",\"email\":\"admin@example.com\",\"password\":\"SenhaSegura123\"}"))
                .andExpect(status().isCreated());
        User admin = users.findByEmailIgnoreCase("admin@example.com").orElseThrow();
        admin.setRole("ADMIN");
        users.save(admin);
        String login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"SenhaSegura123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).path("accessToken").asText();
        mvc.perform(post("/quizzes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quiz Admin JWT\",\"description\":\"Descrição válida\",\"difficulty\":\"FACIL\",\"category\":\"SHONEN\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void administrativeWritesRequireAdminRole() throws Exception {
        mvc.perform(post("/quizzes").with(jwt().jwt(token -> token.subject("1").claim("roles", java.util.List.of("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Teste\",\"description\":\"Descrição válida\",\"difficulty\":\"FACIL\",\"category\":\"SHONEN\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/quizzes").with(jwt().jwt(token -> token.subject("1").claim("roles", java.util.List.of("ADMIN")))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Teste Admin\",\"description\":\"Descrição válida\",\"difficulty\":\"FACIL\",\"category\":\"SHONEN\"}"))
                .andExpect(status().isCreated());
    }
}
