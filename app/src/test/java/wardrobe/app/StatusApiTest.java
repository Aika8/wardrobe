package wardrobe.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code PUT /items/{id}/status} over real HTTP: 200 / 404 / 409 / 400, every body checked against the contract
 * ({@code api/openapi.yaml}). It changes the seed catalogue, so the context is thrown away afterwards; it never
 * touches {@code blue-jeans}, which {@link ItemsApiTest} asserts on.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StatusApiTest {

    static final SchemaRegistry SCHEMAS = SchemaRegistry.withDialect(Dialects.getOpenApi31());
    static final Schema ITEM = SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Item"));
    static final Schema ERROR = SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Error"));

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    final HttpClient http = HttpClient.newHttpClient();

    @Test
    void allowedChangeAnswersTheItemInItsNewStatus() throws Exception {
        HttpResponse<String> res = put("white-shirt", "{\"status\": \"IN_WASH\"}");

        assertEquals(200, res.statusCode(), res.body());
        JsonNode item = valid(ITEM, res.body());
        assertEquals("white-shirt", item.get("id").asString());
        assertEquals("IN_WASH", item.get("status").asString());
        assertEquals("[\"CLEAN\",\"IN_REPAIR\"]", json.writeValueAsString(item.get("next")));
        assertEquals("IN_WASH", itemInList("white-shirt").get("status").asString());
    }

    @Test
    void everyListedItemCarriesTheStatusesItMayChangeTo() throws Exception {
        JsonNode sneakers = itemInList("white-sneakers");
        assertEquals("CLEAN", sneakers.get("status").asString());
        assertEquals("[\"IN_WASH\",\"IN_REPAIR\",\"ON_TRIP\"]", json.writeValueAsString(sneakers.get("next")));
        assertEquals("[\"CLEAN\",\"IN_WASH\"]", json.writeValueAsString(itemInList("green-dress").get("next")));
    }

    @Test
    void unknownItemIs404() throws Exception {
        HttpResponse<String> res = put("no-such-item", "{\"status\": \"CLEAN\"}");

        assertEquals(404, res.statusCode(), res.body());
        JsonNode error = valid(ERROR, res.body());
        assertEquals("UNKNOWN_ITEM", error.get("code").asString());
        assertTrue(error.get("message").asString().contains("no-such-item"), res.body());
    }

    @Test
    void refusedChangeIs409AndLeavesTheItemAsItWas() throws Exception {
        JsonNode before = itemInList("green-dress");
        assertEquals("ON_TRIP", before.get("status").asString());

        HttpResponse<String> res = put("green-dress", "{\"status\": \"IN_REPAIR\"}");

        assertEquals(409, res.statusCode(), res.body());
        JsonNode error = valid(ERROR, res.body());
        assertEquals("ILLEGAL_STATUS_CHANGE", error.get("code").asString());
        String message = error.get("message").asString();
        for (String word : List.of("green-dress", "ON_TRIP", "IN_REPAIR")) {
            assertTrue(message.contains(word), "message names " + word + ": " + message);
        }
        assertEquals(before, itemInList("green-dress"));
    }

    @Test
    void sameStatusIsNotAChangeAnd409() throws Exception {
        HttpResponse<String> res = put("beige-coat", "{\"status\": \"IN_REPAIR\"}");

        assertEquals(409, res.statusCode(), res.body());
        assertEquals("ILLEGAL_STATUS_CHANGE", valid(ERROR, res.body()).get("code").asString());
    }

    @Test
    void statusOutsideTheFourIs400() throws Exception {
        for (String body : List.of("{\"status\": \"LOST\"}", "{\"status\": null}", "{}", "not json", "")) {
            HttpResponse<String> res = put("navy-trousers", body);

            assertEquals(400, res.statusCode(), body + " → " + res.body());
            assertEquals("BAD_REQUEST", valid(ERROR, res.body()).get("code").asString(), body);
        }
        assertEquals("CLEAN", itemInList("navy-trousers").get("status").asString());
    }

    HttpResponse<String> put(String id, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/items/" + id + "/status"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode itemInList(String id) throws Exception {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/items"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), res.body());
        return ItemsApiTest.find(json.readTree(res.body()), id);
    }

    JsonNode valid(Schema schema, String body) throws Exception {
        List<Error> errors = schema.validate(body, InputFormat.JSON);
        assertTrue(errors.isEmpty(), "contract violated: " + errors + " in " + body);
        return json.readTree(body);
    }
}
