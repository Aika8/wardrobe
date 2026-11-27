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
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemId;
import wardrobe.domain.match.Combination;
import wardrobe.domain.outfit.Explain;
import wardrobe.domain.outfit.Outfit;
import wardrobe.domain.outfit.OutfitCheck;
import wardrobe.domain.outfit.OutfitProblem;
import wardrobe.domain.outfit.WhyLine;

/**
 * {@code POST /outfits/check} over real HTTP against the seed catalogue: one {@code why} line per rule, each the
 * domain's {@link Explain} line; the problems are {@link OutfitCheck}'s, in its order, with their messages; {@code ok}
 * is "no problems". Every request and answer is checked against the contract ({@code api/openapi.yaml}). Errors:
 * unknown id 404, empty list or unreadable body 400. CORS preflight for a JSON POST. Read-only.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OutfitCheckApiTest {

    static final SchemaRegistry SCHEMAS = SchemaRegistry.withDialect(Dialects.getOpenApi31());
    static final Schema REQUEST =
            SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/OutfitCheckRequest"));
    static final Schema VERDICT =
            SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/OutfitVerdict"));
    static final Schema ERROR = SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Error"));

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    @Autowired
    ItemCatalog catalog;

    final HttpClient http = HttpClient.newHttpClient();

    /** Outfits of the seed: fine, clashing, not clean, two of a kind, dress with separates, one item, the same id. */
    static Stream<List<String>> outfits() {
        return Stream.of(
                List.of("white-shirt", "navy-trousers"),
                List.of("striped-tee", "navy-trousers", "white-sneakers"),
                List.of("white-shirt", "blue-jeans", "beige-coat"),
                List.of("white-shirt", "striped-tee"),
                List.of("green-dress", "white-shirt", "navy-trousers"),
                List.of("white-shirt"),
                List.of("white-shirt", "white-shirt"));
    }

    @ParameterizedTest
    @MethodSource("outfits")
    void theAnswerIsTheDomainsOwn(List<String> ids) throws Exception {
        JsonNode body = check(ids);
        Outfit outfit = new Outfit(ids.stream().map(id -> catalog.find(new ItemId(id)).orElseThrow()).toList());

        List<WhyLine> lines = Explain.outfit(outfit);
        assertEquals(WhyLine.Rule.values().length, body.get("why").size(), "one line per rule: " + body);
        for (int i = 0; i < lines.size(); i++) {
            WhyLine expected = lines.get(i);
            JsonNode line = body.get("why").get(i);
            assertEquals(expected.rule().name(), line.get("rule").asString(), line.toString());
            assertEquals(expected.goes(), line.get("goes").asBoolean(), line.toString());
            assertEquals(expected.code().name(), line.get("code").asString(), line.toString());
            assertEquals(expected.items().stream().map(ItemId::value).toList(), strings(line.get("items")));
            assertEquals(expected.reason(), line.get("reason").asString());
        }

        List<OutfitProblem> problems = OutfitCheck.check(outfit, Combination.standard());
        assertEquals(problems.size(), body.get("problems").size(), body.toString());
        for (int i = 0; i < problems.size(); i++) {
            assertEquals(problems.get(i).message(), body.get("problems").get(i).get("message").asString());
        }
        assertEquals(problems.isEmpty(), body.get("ok").asBoolean(), body.toString());
    }

    @Test
    void aGoodOutfitIsOkWithEveryLineTicked() throws Exception {
        JsonNode body = check(List.of("white-shirt", "navy-trousers"));

        assertTrue(body.get("ok").asBoolean(), body.toString());
        assertTrue(body.get("problems").isEmpty(), body.toString());
        List<String> rules = new ArrayList<>();
        for (JsonNode line : body.get("why")) {
            rules.add(line.get("rule").asString());
            assertTrue(line.get("goes").asBoolean(), line.toString());
        }
        assertEquals(List.of("FORMALITY", "COLOUR", "PATTERN", "SEASON", "CLEAN"), rules);
    }

    @Test
    void eachProblemHasItsCodeAndItsItems() throws Exception {
        assertProblem(List.of("white-shirt", "blue-jeans"), "NOT_CLEAN", List.of("blue-jeans"));
        assertProblem(List.of("white-shirt", "striped-tee"), "TWO_OF_A_KIND", List.of("white-shirt", "striped-tee"));
        assertProblem(List.of("green-dress", "navy-trousers"), "DRESS_WITH_SEPARATES",
                List.of("green-dress", "navy-trousers"));
        assertProblem(List.of("striped-tee", "navy-trousers"), "CLASH", List.of("striped-tee", "navy-trousers"));
    }

    @Test
    void aClashIsAFailedRuleLineNamingThePair() throws Exception {
        // Striped T-shirt (CASUAL, summer) and navy trousers (BUSINESS, autumn–spring).
        JsonNode body = check(List.of("striped-tee", "navy-trousers"));

        assertFalse(body.get("ok").asBoolean(), body.toString());
        JsonNode formality = body.get("why").get(0);
        assertEquals("FORMALITY", formality.get("rule").asString());
        assertFalse(formality.get("goes").asBoolean(), formality.toString());
        assertEquals("STEPS_APART", formality.get("code").asString());
        assertEquals(List.of("striped-tee", "navy-trousers"), strings(formality.get("items")));
        JsonNode season = body.get("why").get(3);
        assertEquals("NO_SHARED_SEASON", season.get("code").asString(), season.toString());
    }

    @Test
    void anItemNotCleanIsAProblemNotAnError() throws Exception {
        JsonNode body = check(List.of("blue-jeans")); // IN_WASH in the seed

        assertFalse(body.get("ok").asBoolean(), body.toString());
        JsonNode clean = body.get("why").get(4);
        assertEquals("CLEAN", clean.get("rule").asString());
        assertEquals("NOT_CLEAN", clean.get("code").asString());
        assertEquals("NO_PAIRS", body.get("why").get(0).get("code").asString());
    }

    @Test
    void unknownIdIs404() throws Exception {
        HttpResponse<String> res = post("{\"items\": [\"white-shirt\", \"no-such-item\"]}");

        assertEquals(404, res.statusCode(), res.body());
        assertError(res, "UNKNOWN_ITEM");
        assertTrue(res.body().contains("no-such-item"), res.body());
    }

    @Test
    void badBodiesAre400() throws Exception {
        for (String body : List.of("{\"items\": []}", "{}", "{\"items\": null}", "{\"items\": [\"\"]}",
                "{\"items\": [\" \"]}", "{\"items\": [null]}", "not json", "", "{\"items\": \"white-shirt\"}")) {
            HttpResponse<String> res = post(body);
            assertEquals(400, res.statusCode(), body + " → " + res.body());
            assertError(res, "BAD_REQUEST");
        }
    }

    @Test
    void theEmptyListIsAgainstTheContractToo() {
        assertFalse(REQUEST.validate("{\"items\": []}", InputFormat.JSON).isEmpty());
    }

    @Test
    void localOriginMayPostJson() throws Exception {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(uri())
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, res.statusCode(), res.body());
        assertEquals("http://localhost:8081", res.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertTrue(res.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"),
                res.headers().map().toString());
    }

    void assertProblem(List<String> ids, String code, List<String> items) throws Exception {
        JsonNode body = check(ids);
        for (JsonNode problem : body.get("problems")) {
            if (problem.get("code").asString().equals(code)) {
                assertEquals(items, strings(problem.get("items")), problem.toString());
                return;
            }
        }
        throw new AssertionError("no " + code + " in " + body);
    }

    void assertError(HttpResponse<String> res, String code) throws Exception {
        List<Error> errors = ERROR.validate(res.body(), InputFormat.JSON);
        assertTrue(errors.isEmpty(), "contract violated: " + errors + " in " + res.body());
        assertEquals(code, json.readTree(res.body()).get("code").asString());
    }

    JsonNode check(List<String> ids) throws Exception {
        String request = json.writeValueAsString(Map.of("items", ids));
        List<Error> requestErrors = REQUEST.validate(request, InputFormat.JSON);
        assertTrue(requestErrors.isEmpty(), "OutfitCheckRequest violated: " + requestErrors + " in " + request);
        HttpResponse<String> res = post(request);
        assertEquals(200, res.statusCode(), request + " → " + res.body());
        List<Error> errors = VERDICT.validate(res.body(), InputFormat.JSON);
        assertTrue(errors.isEmpty(), "OutfitVerdict violated: " + errors + " in " + res.body());
        return json.readTree(res.body());
    }

    HttpResponse<String> post(String body) throws Exception {
        return http.send(HttpRequest.newBuilder(uri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    URI uri() {
        return URI.create("http://localhost:" + port + "/outfits/check");
    }

    static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(v -> values.add(v.asString()));
        return values;
    }
}
