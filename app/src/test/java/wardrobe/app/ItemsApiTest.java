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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code GET /items} over real HTTP: every item matches the contract ({@code api/openapi.yaml}, schema {@code Item}),
 * and the list is what the catalogue holds.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ItemsApiTest {

    static final SchemaRegistry SCHEMAS = SchemaRegistry.withDialect(Dialects.getOpenApi31());

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    final HttpClient http = HttpClient.newHttpClient();

    @Test
    void itemsAnswerAsTheContractSays() throws Exception {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/items"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), res.body());
        JsonNode array = json.readTree(res.body());
        assertTrue(array.isArray() && !array.isEmpty(), res.body());

        Schema schema = SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Item"));
        List<String> ids = new ArrayList<>();
        for (JsonNode item : array) {
            List<Error> errors = schema.validate(json.writeValueAsString(item), InputFormat.JSON);
            assertTrue(errors.isEmpty(), "Item violated: " + errors + " in " + item);
            ids.add(item.get("id").asString());
        }
        assertEquals(ids.stream().distinct().count(), ids.size(), "ids are unique: " + ids);
        JsonNode jeans = find(array, "blue-jeans");
        assertEquals("IN_WASH", jeans.get("status").asString());
        assertEquals("[\"WINTER\",\"SPRING\",\"SUMMER\",\"AUTUMN\"]", json.writeValueAsString(jeans.get("seasons")));
    }

    static JsonNode find(JsonNode array, String id) {
        for (JsonNode item : array) {
            if (id.equals(item.get("id").asString())) {
                return item;
            }
        }
        throw new AssertionError("no item " + id + " in " + array);
    }
}
