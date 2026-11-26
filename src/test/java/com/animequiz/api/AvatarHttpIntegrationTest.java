package com.animequiz.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises Tomcat's real multipart PUT parser, not MockMvc's synthetic MultipartFile. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.catalog.seed-enabled=false", "api.auth-limit.enabled=false",
    "spring.servlet.multipart.max-file-size=2MB", "spring.servlet.multipart.max-request-size=2200KB"
})
@EnabledIfSystemProperty(named = "animequiz.http-tests", matches = "true")
class AvatarHttpIntegrationTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    private String register() throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "");
        var body = json.writeValueAsString(Map.of("username", "photo" + id,
            "email", id + "@example.test", "password", "ExamplePass9!"));
        var response = client.send(HttpRequest.newBuilder(uri("/auth/register"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), response.body());
        return json.readTree(response.body()).path("accessToken").asText();
    }
    private HttpResponse<byte[]> upload(String token, byte[] photo) throws Exception {
        String boundary = "animequiz-avatar-http-test";
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"avatar.png\"\r\n"
            + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(photo);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(uri("/users/me/avatar"))
            .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + token)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .PUT(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test void realMultipartPutPersistsPhotoAndRequiresBearerToReadIt() throws Exception {
        String token = register();
        var buffer = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", buffer);
        byte[] photo = buffer.toByteArray();
        var result = upload(token, photo);
        assertEquals(200, result.statusCode(), new String(result.body(), StandardCharsets.UTF_8));
        assertTrue(json.readTree(result.body()).path("hasAvatar").asBoolean());
        var downloaded = client.send(HttpRequest.newBuilder(uri("/users/me/avatar"))
            .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, downloaded.statusCode());
        assertArrayEquals(photo, downloaded.body());
        assertEquals("image/png", downloaded.headers().firstValue("Content-Type").orElseThrow());
        var anonymous = client.send(HttpRequest.newBuilder(uri("/users/me/avatar")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, anonymous.statusCode());
    }

    @Test void oversizedRealMultipartReturnsJson413InsteadOfDroppingTheConnection() throws Exception {
        var response = upload(register(), new byte[2 * 1024 * 1024 + 1]);
        assertEquals(413, response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
        assertEquals(413, json.readTree(response.body()).path("status").asInt());
    }
}
