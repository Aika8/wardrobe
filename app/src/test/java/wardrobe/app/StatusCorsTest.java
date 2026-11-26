package wardrobe.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** The Expo web build (another local port) may send {@code PUT /items/{id}/status}; other origins may not. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatusCorsTest {

    @LocalServerPort
    int port;

    final HttpClient http = HttpClient.newHttpClient();

    @Test
    void localOriginMayPut() throws Exception {
        HttpResponse<String> res = preflight("http://localhost:8081");

        assertEquals(200, res.statusCode(), res.body());
        assertEquals("http://localhost:8081", res.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertTrue(res.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("PUT"),
                res.headers().map().toString());
    }

    @Test
    void foreignOriginMayNot() throws Exception {
        HttpResponse<String> res = preflight("http://example.org");

        assertEquals(403, res.statusCode(), res.body());
        assertTrue(res.headers().firstValue("Access-Control-Allow-Origin").isEmpty(), res.headers().map().toString());
    }

    HttpResponse<String> preflight(String origin) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/items/white-shirt/status"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "PUT")
                .build(), HttpResponse.BodyHandlers.ofString());
    }
}
