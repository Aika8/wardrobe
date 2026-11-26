package wardrobe.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * {@code GET /items?status=…} over real HTTP: only the items in that status, in the catalogue's order; without the
 * parameter everything as before; a status that is not one of the four is a 400 {@code Error}. Every body is checked
 * against the contract ({@code api/openapi.yaml}). Read-only: it never changes the catalogue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FilterApiTest {

    static final SchemaRegistry SCHEMAS = SchemaRegistry.withDialect(Dialects.getOpenApi31());
    static final Schema ITEM = SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Item"));
    static final Schema ERROR = SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Error"));

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    final HttpClient http = HttpClient.newHttpClient();

    @Test
    void cleanReturnsOnlyCleanItemsInCatalogueOrder() throws Exception {
        List<JsonNode> clean = items("?status=CLEAN");

        assertFalse(clean.isEmpty());
        for (JsonNode item : clean) {
            assertEquals("CLEAN", item.get("status").asString(), item.toString());
        }
        List<String> ids = ids(clean);
        assertTrue(ids.contains("white-sneakers"), ids.toString());
        assertFalse(ids.contains("blue-jeans"), ids.toString());
        assertEquals(idsIn(items(""), "CLEAN"), ids);
    }

    @Test
    void everyStatusSplitsTheWholeList() throws Exception {
        List<JsonNode> all = items("");
        int total = 0;
        for (String status : List.of("CLEAN", "IN_WASH", "IN_REPAIR", "ON_TRIP")) {
            List<JsonNode> some = items("?status=" + status);
            assertEquals(idsIn(all, status), ids(some), status);
            total += some.size();
        }
        assertEquals(all.size(), total);
        assertEquals(List.of("blue-jeans"), idsIn(all, "IN_WASH"));
    }

    @Test
    void withoutTheParameterEveryItemAsBefore() throws Exception {
        List<String> all = ids(items(""));
        assertTrue(all.containsAll(List.of("white-shirt", "blue-jeans", "beige-coat", "green-dress")), all.toString());
        assertEquals(all, ids(items("?status=")), "an empty status means no filter");
        assertEquals(all, ids(items("?status=%20")), "a blank status means no filter");
    }

    @Test
    void statusOutsideTheFourIs400() throws Exception {
        for (String query : List.of("?status=LOST", "?status=clean", "?status=CLEAN,IN_WASH")) {
            HttpResponse<String> res = get(query);

            assertEquals(400, res.statusCode(), query + " → " + res.body());
            List<Error> errors = ERROR.validate(res.body(), InputFormat.JSON);
            assertTrue(errors.isEmpty(), "contract violated: " + errors + " in " + res.body());
            JsonNode error = json.readTree(res.body());
            assertEquals("BAD_REQUEST", error.get("code").asString(), query);
            assertTrue(error.get("message").asString().contains("status"), res.body());
        }
    }

    List<JsonNode> items(String query) throws Exception {
        HttpResponse<String> res = get(query);
        assertEquals(200, res.statusCode(), query + " → " + res.body());
        JsonNode array = json.readTree(res.body());
        assertTrue(array.isArray(), res.body());
        List<JsonNode> list = new ArrayList<>();
        for (JsonNode item : array) {
            List<Error> errors = ITEM.validate(json.writeValueAsString(item), InputFormat.JSON);
            assertTrue(errors.isEmpty(), "Item violated: " + errors + " in " + item);
            list.add(item);
        }
        return list;
    }

    HttpResponse<String> get(String query) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/items" + query)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    static List<String> ids(List<JsonNode> items) {
        return items.stream().map(item -> item.get("id").asString()).toList();
    }

    static List<String> idsIn(List<JsonNode> items, String status) {
        return ids(items.stream().filter(item -> status.equals(item.get("status").asString())).toList());
    }
}
