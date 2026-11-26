package com.animequiz.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private JsonNode document(String version) throws Exception {
        return json.readTree(mvc.perform(get("/api-docs" + version)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
    }

    @Test void v1HasOnlyPublicQueriesWithoutSecurityOrRateLimiting() throws Exception {
        var doc=document("/v1");
        assertThat(doc.path("info").path("version").asText()).isEqualTo("1.0.0");
        assertThat(doc.path("components").path("securitySchemes").size()).isZero();
        doc.path("paths").fields().forEachRemaining(entry -> {
            assertThat(entry.getValue().size()).isEqualTo(1);
            var op=entry.getValue().path("get");
            assertThat(op.isMissingNode()).isFalse();
            assertThat(op.path("security").size()).isZero();
            for(String code:List.of("401","403","429")) assertThat(op.path("responses").has(code)).isFalse();
        });
        assertThat(doc.path("paths").has("/quizzes")).isTrue();
        assertThat(doc.path("paths").has("/quizzes/{quizId}/questions")).isTrue();
        assertThat(doc.path("paths").has("/users/me")).isFalse();
    }

    @Test void v2UsesOnlyBearerAndDocumentsNormalPlayerOperations() throws Exception {
        var doc=document("/v2");
        var names=new HashSet<String>();doc.path("components").path("securitySchemes").fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactly("bearerAuth");
        assertThat(doc.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        var paths=doc.path("paths");
        for(String path:List.of("/users/me","/users/me/settings","/users/me/avatar","/users/me/favorites",
            "/quizzes/{quizId}/attempts","/scores/ranking","/scores/{id}")) assertThat(paths.has(path)).isTrue();
        assertThat(paths.path("/quizzes").path("get").path("security").get(0).has("bearerAuth")).isTrue();
        assertThat(paths.path("/auth/login").path("post").path("security").size()).isZero();
        assertThat(paths.path("/users/me/avatar").path("put").path("responses").path("413")
            .path("content").path("application/json").path("example").path("status").asInt()).isEqualTo(413);
    }

    @Test void deferredFeaturesStayHiddenAndOnlyV2DocumentsProtectedAdministration() throws Exception {
        for(String version:List.of("","/v1","/v2")){
            var paths=document(version).path("paths");
            for(String path:List.of("/api-keys","/api-keys/verify","/users","/users/{id}","/profiles",
                "/auth/google","/auth/google/link","/auth/forgot-password","/auth/reset-password")) assertThat(paths.has(path)).isFalse();
            for(String path:List.of("/quizzes","/quizzes/{id}","/questions","/questions/{id}","/options","/options/{id}")){
                for(String method:List.of("post","put","delete","patch")) if (version.equals("/v1")) assertThat(paths.path(path).has(method)).isFalse();
            }
            assertThat(paths.has("/scores")).isFalse();
        }
    }

    @Test void versionHeadersAndQuestionSchemasRemainDifferentWithoutAnswerKeys() throws Exception {
        for(String version:List.of("1","2")){
            var doc=document("/v"+version);
            var parameters=doc.path("paths").path("/quizzes").path("get").path("parameters");
            boolean found=false;
            for(var parameter:parameters) if(parameter.path("name").asText().equals("X-API-Version")){
                found=true;assertThat(parameter.path("schema").path("default").asText()).isEqualTo(version);
            }
            assertThat(found).isTrue();
            for(var schema:doc.path("components").path("schemas")) {
                if (!schema.equals(doc.path("components").path("schemas").path("OptionRequest")))
                    assertThat(schema.toString()).doesNotContain("\"isCorrect\"");
                assertThat(schema.path("properties").has("correct")).isFalse();
            }
        }
        assertThat(document("/v1").path("components").path("schemas").toString()).doesNotContain("QuestionWithOptionsV2Response");
        assertThat(document("/v2").path("components").path("schemas").toString()).contains("imageAltText","timeLimitSeconds");
    }

    @Test void hiddenSchemasArePrunedAndEveryRemainingReferenceResolves() throws Exception {
        for(String version:List.of("/v1","/v2")){
            var doc=document(version);
            var schemas=doc.path("components").path("schemas");
            var names=new ArrayList<String>();schemas.fieldNames().forEachRemaining(names::add);
            assertThat(names).noneMatch(name->name.contains("GoogleRequest")||name.contains("CreatedKey")||name.contains("ResetRequest")||name.equals("UserRequest"));
            verifyReferences(doc,doc);
        }
    }
    private void verifyReferences(JsonNode node,JsonNode root){
        if(node.isObject()&&node.path("$ref").asText().startsWith("#/components/")){
            assertThat(root.at(node.path("$ref").asText().substring(1)).isMissingNode()).isFalse();
        }
        node.elements().forEachRemaining(child->verifyReferences(child,root));
    }

    @Test void v2DocumentsRateLimitsErrorsAndIdempotencyOnTheApplicableRoutes() throws Exception {
        var paths=document("/v2").path("paths");
        var limited=paths.path("/auth/login").path("post").path("responses").path("429");
        assertThat(limited.path("headers").has("Retry-After")).isTrue();
        assertThat(limited.path("headers").has("X-AuthRateLimit-Reset")).isTrue();
        var responses=paths.path("/quizzes/{quizId}/attempts").path("post").path("responses");
        for(String status:List.of("201","200","400","401","409","429")) assertThat(responses.has(status)).isTrue();
        assertThat(paths.path("/auth/register").path("post").path("responses").has("201")).isTrue();
    }

    @Test void documentationLinksKeepTheOriginalPresentationRoutes() throws Exception {
        for(String path:List.of("/","/docs","/docs.html","/docs/v1")) mvc.perform(get(path))
            .andExpect(status().isFound()).andExpect(redirectedUrl("/swagger-ui.html"));
        mvc.perform(get("/docs/v2")).andExpect(status().isOk()).andExpect(forwardedUrl("/docs-v2.html"));
    }
}
