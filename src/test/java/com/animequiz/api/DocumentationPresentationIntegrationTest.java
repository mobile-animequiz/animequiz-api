package com.animequiz.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentationPresentationIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void v1EntryPointsKeepStandardSwaggerAndLegacyV2LinksRemainUsable() throws Exception {
        for (String path : new String[]{"/", "/docs", "/docs.html", "/docs/v1"}) {
            mvc.perform(get(path)).andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui.html"))
                .andExpect(header().string("Cache-Control", "no-store"));
        }
        for (String path : new String[]{"/docs", "/docs.html"}) {
            mvc.perform(get(path).param("version", "2")).andExpect(status().isFound())
                .andExpect(redirectedUrl("/docs/v2"));
        }
    }

    @Test void v1LoadsTheStockSwaggerStylesWithoutTheV2Background() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("swagger-ui.css")))
            .andExpect(content().string(containsString("/docs-version-switch.js")))
            .andExpect(content().string(not(containsString("video-fundo"))))
            .andExpect(content().string(not(containsString("fonts.googleapis.com"))))
            .andExpect(content().string(not(containsString("--painel"))));
    }

    @Test void v2RestoresTheHistoricalThemeAndUsesOnlyContractTwo() throws Exception {
        mvc.perform(get("/docs/v2")).andExpect(status().isOk())
            .andExpect(forwardedUrl("/docs-v2.html"));
        mvc.perform(get("/docs-v2.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("--painel:rgba(8,8,12,.86)")))
            .andExpect(content().string(containsString("video-fundo")))
            .andExpect(content().string(containsString("value=\"2\" selected")))
            .andExpect(content().string(containsString("const version = '2';")))
            .andExpect(content().string(containsString("request.headers['X-API-Version'] = version")))
            .andExpect(content().string(containsString("window.location.assign('/swagger-ui.html')")));
    }

    @Test void documentationResourcesArePublicAndDoNotConsumeTheApiRateLimit() throws Exception {
        for (String path : new String[]{"/docs/v2", "/docs-v2.html", "/docs-version-switch.js"}) {
            mvc.perform(get(path).servletPath(path).header("X-API-Version", "unsupported"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-RateLimit-Limit"));
        }
    }
}
