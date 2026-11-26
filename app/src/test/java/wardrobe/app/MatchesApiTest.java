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
import wardrobe.domain.Item;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemId;
import wardrobe.domain.match.Combination;

/**
 * {@code GET /items/{id}/matches} over real HTTP against the seed catalogue: both lists hold exactly the clean items
 * other than the item itself, in catalogue order, each with the domain's verdict and reason; unknown id → 404, blank
 * id → 400. Every body is checked against the contract ({@code api/openapi.yaml}). Read-only.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MatchesApiTest {

    static final SchemaRegistry SCHEMAS = SchemaRegistry.withDialect(Dialects.getOpenApi31());
    static final Schema MATCHES =
            SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Matches"));
    static final Schema ERROR = SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Error"));

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    @Autowired
    ItemCatalog catalog;

    final HttpClient http = HttpClient.newHttpClient();

    @Test
    void everyOtherCleanItemIsJudgedOnceInCatalogueOrder() throws Exception {
        for (Item item : catalog.all()) {
            JsonNode body = matches(item.id().value());

            assertEquals(item.id().value(), body.get("item").get("id").asString());
            List<String> expected = catalog.all().stream()
                    .filter(other -> other.availableForOutfit() && !other.id().equals(item.id()))
                    .map(other -> other.id().value()).toList();
            List<String> goes = ids(body.get("goes"));
            List<String> doesNot = ids(body.get("doesNot"));
            assertFalse(goes.contains(item.id().value()), body.toString());
            assertFalse(doesNot.contains(item.id().value()), body.toString());
            List<String> both = new ArrayList<>(goes);
            both.addAll(doesNot);
            assertEquals(expected.size(), both.size(), body.toString());
            assertTrue(both.containsAll(expected), body.toString());
            assertEquals(expected.stream().filter(goes::contains).toList(), goes, "goes in catalogue order");
            assertEquals(expected.stream().filter(doesNot::contains).toList(), doesNot, "doesNot in catalogue order");
            for (JsonNode match : listed(body)) {
                assertEquals("CLEAN", match.get("item").get("status").asString(), match.toString());
            }
        }
    }

    @Test
    void verdictsAndReasonsAreTheDomainsOwn() throws Exception {
        Combination standard = Combination.standard();
        for (Item item : catalog.all()) {
            JsonNode body = matches(item.id().value());
            for (String list : List.of("goes", "doesNot")) {
                for (JsonNode match : body.get(list)) {
                    Item other = catalog.find(new ItemId(match.get("item").get("id").asString())).orElseThrow();
                    var verdict = standard.judge(item, other);
                    assertEquals(list.equals("goes"), verdict.goes(), item.id() + " with " + other.id());
                    assertEquals(verdict.reason(), match.get("reason").asString());
                }
            }
        }
    }

    @Test
    void anItemNotCleanMayStillBeAsked() throws Exception {
        JsonNode body = matches("blue-jeans"); // IN_WASH in the seed

        assertEquals("IN_WASH", body.get("item").get("status").asString());
        assertFalse(listed(body).isEmpty(), body.toString());
    }

    @Test
    void aPairTwoRulesRejectShowsBothReasons() throws Exception {
        // Striped T-shirt (CASUAL, summer) and navy trousers (BUSINESS, autumn–spring): formality and season refuse.
        JsonNode body = matches("striped-tee");

        JsonNode trousers = null;
        for (JsonNode match : body.get("doesNot")) {
            if (match.get("item").get("id").asString().equals("navy-trousers")) {
                trousers = match;
            }
        }
        assertTrue(trousers != null, body.toString());
        String reason = trousers.get("reason").asString();
        assertTrue(reason.contains("formality CASUAL and BUSINESS are 2 steps apart"), reason);
        assertTrue(reason.contains("do not overlap"), reason);
        assertTrue(reason.contains("; "), reason);
    }

    @Test
    void unknownIdIs404AndBlankIdIs400() throws Exception {
        HttpResponse<String> unknown = get("no-such-item");
        assertEquals(404, unknown.statusCode(), unknown.body());
        assertError(unknown, "UNKNOWN_ITEM");

        HttpResponse<String> blank = get("%20");
        assertEquals(400, blank.statusCode(), blank.body());
        assertError(blank, "BAD_REQUEST");
    }

    void assertError(HttpResponse<String> res, String code) throws Exception {
        List<Error> errors = ERROR.validate(res.body(), InputFormat.JSON);
        assertTrue(errors.isEmpty(), "contract violated: " + errors + " in " + res.body());
        assertEquals(code, json.readTree(res.body()).get("code").asString());
    }

    JsonNode matches(String id) throws Exception {
        HttpResponse<String> res = get(id);
        assertEquals(200, res.statusCode(), id + " → " + res.body());
        List<Error> errors = MATCHES.validate(res.body(), InputFormat.JSON);
        assertTrue(errors.isEmpty(), "Matches violated: " + errors + " in " + res.body());
        return json.readTree(res.body());
    }

    HttpResponse<String> get(String id) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/items/" + id + "/matches"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    static List<JsonNode> listed(JsonNode body) {
        List<JsonNode> all = new ArrayList<>();
        body.get("goes").forEach(all::add);
        body.get("doesNot").forEach(all::add);
        return all;
    }

    static List<String> ids(JsonNode matches) {
        List<String> ids = new ArrayList<>();
        matches.forEach(match -> ids.add(match.get("item").get("id").asString()));
        return ids;
    }
}
