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
import wardrobe.domain.ItemType;
import wardrobe.domain.avatar.Figure;
import wardrobe.domain.avatar.Fit;
import wardrobe.domain.avatar.FitNote;
import wardrobe.domain.avatar.GarmentSize;
import wardrobe.domain.avatar.Measurements;
import wardrobe.domain.avatar.Placement;

/**
 * {@code POST /avatar/dress} over real HTTP against the seed catalogue, with invented figures: the figure's
 * landmarks, one shape per item in drawing order, the fit notes — each the domain's own answer — and every body
 * (request and response) checked against the contract ({@code api/openapi.yaml}). Errors: unknown item 404, bad
 * measurement or size 400 naming the field, impossible outfit 409. CORS preflight for a JSON POST. Read-only.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AvatarApiTest {

    static final SchemaRegistry SCHEMAS = SchemaRegistry.withDialect(Dialects.getOpenApi31());
    static final Schema REQUEST =
            SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/DressRequest"));
    static final Schema DRESSED =
            SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/DressedAvatar"));
    static final Schema ERROR = SCHEMAS.getSchema(SchemaLocation.of("classpath:openapi.yaml#/components/schemas/Error"));

    /** Invented figures: the corners of the ranges and one in the middle. */
    static Stream<Measurements> figures() {
        return Stream.of(
                new Measurements(1650, 380, 880, 700, 960, 580, 780),
                new Measurements(1300, 300, 600, 450, 650, 450, 1000),
                new Measurements(1300, 600, 1600, 1500, 1700, 800, 600),
                new Measurements(2200, 300, 600, 450, 650, 450, 600),
                new Measurements(2200, 600, 1600, 1500, 1700, 800, 1000));
    }

    static final Measurements MIDDLE = new Measurements(1650, 380, 880, 700, 960, 580, 780);

    static final String SHIRT_SIZE = "{\"chest\": 960, \"waist\": 800, \"length\": 700, \"sleeve\": 600}";
    static final String TROUSERS_SIZE = "{\"waist\": 720, \"hips\": 1000, \"inseam\": 790}";

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    @Autowired
    ItemCatalog catalog;

    final HttpClient http = HttpClient.newHttpClient();

    @ParameterizedTest
    @MethodSource("figures")
    void answerIsTheDomainsFigureShapesAndFitInDrawingOrder(Measurements m) throws Exception {
        String body = request(m, item("white-shirt", SHIRT_SIZE), item("navy-trousers", TROUSERS_SIZE),
                item("white-sneakers", null));

        JsonNode answer = dressed(body);

        Figure figure = Figure.of(m);
        JsonNode landmarks = answer.get("figure").get("landmarks");
        List<String> names = List.of("head", "neck", "shoulders", "chest", "waist", "hips", "crotch", "knee", "ankle",
                "floor");
        int[] expected = figure.landmarks();
        for (int i = 0; i < names.size(); i++) {
            assertEquals(expected[i], landmarks.get(names.get(i)).asInt(), names.get(i));
        }
        JsonNode halfWidths = answer.get("figure").get("halfWidths");
        assertEquals(figure.shoulderHalfWidth(), halfWidths.get("shoulders").asInt());
        assertEquals(figure.chestHalfWidth(), halfWidths.get("chest").asInt());
        assertEquals(figure.waistHalfWidth(), halfWidths.get("waist").asInt());
        assertEquals(figure.hipHalfWidth(), halfWidths.get("hips").asInt());

        assertEquals(List.of("white-sneakers", "navy-trousers", "white-shirt"), itemIds(answer));
        GarmentSize shirt = GarmentSize.empty().withChest(960).withWaist(800).withLength(700).withSleeve(600);
        GarmentSize trousers = GarmentSize.empty().withWaist(720).withHips(1000).withInseam(790);
        assertLayer(answer.get("layers").get(0), m, ItemType.SHOES, GarmentSize.empty());
        assertLayer(answer.get("layers").get(1), m, ItemType.BOTTOM, trousers);
        assertLayer(answer.get("layers").get(2), m, ItemType.TOP, shirt);
    }

    @Test
    void drawingOrderDoesNotDependOnTheRequestOrder() throws Exception {
        JsonNode answer = dressed(request(MIDDLE, item("white-sneakers", null), item("white-shirt", SHIRT_SIZE),
                item("navy-trousers", TROUSERS_SIZE)));

        assertEquals(List.of("white-sneakers", "navy-trousers", "white-shirt"), itemIds(answer));
    }

    @Test
    void aSizeWithoutSleeveLeavesTheSleeveEndOutAndATightShirtSaysWhere() throws Exception {
        JsonNode answer = dressed(request(MIDDLE, item("white-shirt", "{\"chest\": 850}")));

        JsonNode layer = answer.get("layers").get(0);
        assertFalse(layer.get("shape").has("sleeveEnd"), layer.toString());
        assertEquals(1, layer.get("fit").size(), layer.toString());
        assertEquals("TOO_TIGHT", layer.get("fit").get(0).get("kind").asString());
        assertEquals("CHEST", layer.get("fit").get(0).get("part").asString());
    }

    @Test
    void itemsNotCleanAreDrawnAndAnEmptyOutfitIsAnUndressedFigure() throws Exception {
        // Green dress ON_TRIP, beige coat IN_REPAIR in the seed: shown anyway, as the domain's Layers decides.
        JsonNode answer = dressed(request(MIDDLE, item("beige-coat", null), item("green-dress", null)));
        assertEquals(List.of("green-dress", "beige-coat"), itemIds(answer));

        JsonNode empty = dressed(request(MIDDLE));
        assertTrue(empty.get("layers").isEmpty(), empty.toString());
        assertTrue(empty.get("figure").has("landmarks"), empty.toString());
    }

    @Test
    void unknownItemIs404() throws Exception {
        HttpResponse<String> res = post(request(MIDDLE, item("white-shirt", null), item("no-such-item", null)));

        assertEquals(404, res.statusCode(), res.body());
        assertTrue(error(res, "UNKNOWN_ITEM").contains("no-such-item"), res.body());
    }

    @Test
    void anImpossibleOutfitIs409WithTheProblems() throws Exception {
        HttpResponse<String> twoTops = post(request(MIDDLE, item("white-shirt", null), item("striped-tee", null)));
        assertEquals(409, twoTops.statusCode(), twoTops.body());
        String message = error(twoTops, "IMPOSSIBLE_OUTFIT");
        assertTrue(message.contains("White shirt") && message.contains("Striped T-shirt"), message);

        HttpResponse<String> dressAndSeparates = post(request(MIDDLE, item("green-dress", null),
                item("white-shirt", null), item("navy-trousers", null)));
        assertEquals(409, dressAndSeparates.statusCode(), dressAndSeparates.body());
        String both = error(dressAndSeparates, "IMPOSSIBLE_OUTFIT");
        assertTrue(both.contains("White shirt") && both.contains("Navy trousers") && both.contains("; "), both);
    }

    @Test
    void theSameItemTwiceIsTwoOfAKind() throws Exception {
        HttpResponse<String> res = post(request(MIDDLE, item("white-shirt", null), item("white-shirt", null)));

        assertEquals(409, res.statusCode(), res.body());
        error(res, "IMPOSSIBLE_OUTFIT");
    }

    @Test
    void badMeasurementsAre400NamingTheField() throws Exception {
        String items = "\"items\": []";
        assertBadRequest("{\"measurements\": {\"height\": 1650, \"shoulders\": 380, \"chest\": 880, \"waist\": 100,"
                + " \"hips\": 960, \"arm\": 580, \"inseam\": 780}, " + items + "}", "measurements.waist");
        assertBadRequest("{\"measurements\": {\"height\": 1650, \"shoulders\": 380, \"chest\": 880, \"waist\": 700,"
                + " \"hips\": 960, \"arm\": 580}, " + items + "}", "measurements.inseam");
        assertBadRequest("{\"measurements\": {\"height\": 1650.5, \"shoulders\": 380, \"chest\": 880, \"waist\": 700,"
                + " \"hips\": 960, \"arm\": 580, \"inseam\": 780}, " + items + "}", "measurements.height");
        assertBadRequest("{\"measurements\": {\"height\": 99999999999, \"shoulders\": 380, \"chest\": 880,"
                + " \"waist\": 700, \"hips\": 960, \"arm\": 580, \"inseam\": 780}, " + items + "}",
                "measurements.height");
        assertBadRequest("{" + items + "}", "measurements");
    }

    @Test
    void badSizesItemsAndBodiesAre400() throws Exception {
        assertBadRequest(request(MIDDLE, item("white-shirt", "{\"chest\": -5}")), "items[0].size.chest");
        assertBadRequest(request(MIDDLE, item("white-shirt", null), item("navy-trousers", "{\"inseam\": 780.5}")),
                "items[1].size.inseam");
        assertBadRequest(request(MIDDLE, "{\"size\": {}}"), "items[0].id");
        assertBadRequest("{\"measurements\": " + measurements(MIDDLE) + "}", "items");
        assertBadRequest("not json", "JSON");
    }

    @Test
    void localOriginMayPostJson() throws Exception {
        HttpResponse<String> res = preflight("http://localhost:8081");

        assertEquals(200, res.statusCode(), res.body());
        assertEquals("http://localhost:8081", res.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertTrue(res.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"),
                res.headers().map().toString());
        assertTrue(res.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase()
                .contains("content-type"), res.headers().map().toString());
    }

    @Test
    void foreignOriginMayNotPost() throws Exception {
        HttpResponse<String> res = preflight("http://example.org");

        assertEquals(403, res.statusCode(), res.body());
        assertTrue(res.headers().firstValue("Access-Control-Allow-Origin").isEmpty(), res.headers().map().toString());
    }

    void assertLayer(JsonNode layer, Measurements m, ItemType type, GarmentSize size) {
        assertEquals(type.name(), layer.get("type").asString(), layer.toString());
        Placement.Shape shape = Placement.of(Figure.of(m), type, size);
        JsonNode s = layer.get("shape");
        assertEquals(shape.top(), s.get("top").asInt(), layer.toString());
        assertEquals(shape.bottom(), s.get("bottom").asInt(), layer.toString());
        assertEquals(shape.topHalfWidth(), s.get("topHalfWidth").asInt(), layer.toString());
        assertEquals(shape.bottomHalfWidth(), s.get("bottomHalfWidth").asInt(), layer.toString());
        assertEquals(shape.clamped(), s.get("clamped").asBoolean(), layer.toString());
        assertEquals(shape.sleeveEnd().isPresent(), s.has("sleeveEnd"), layer.toString());
        if (shape.sleeveEnd().isPresent()) {
            assertEquals(shape.sleeveEnd().getAsInt(), s.get("sleeveEnd").asInt(), layer.toString());
        }
        List<FitNote> notes = Fit.check(m, type, size);
        assertEquals(notes.size(), layer.get("fit").size(), layer.toString());
        for (int i = 0; i < notes.size(); i++) {
            assertEquals(notes.get(i).kind().name(), layer.get("fit").get(i).get("kind").asString());
            assertEquals(notes.get(i).part().name(), layer.get("fit").get(i).get("part").asString());
        }
        String id = layer.get("itemId").asString();
        assertEquals(type, catalog.find(new ItemId(id)).orElseThrow().type(), id);
    }

    void assertBadRequest(String body, String named) throws Exception {
        HttpResponse<String> res = post(body);
        assertEquals(400, res.statusCode(), body + " → " + res.body());
        String message = error(res, "BAD_REQUEST");
        assertTrue(message.contains(named), "message names " + named + ": " + message);
    }

    String error(HttpResponse<String> res, String code) throws Exception {
        List<Error> errors = ERROR.validate(res.body(), InputFormat.JSON);
        assertTrue(errors.isEmpty(), "contract violated: " + errors + " in " + res.body());
        JsonNode error = json.readTree(res.body());
        assertEquals(code, error.get("code").asString(), res.body());
        return error.get("message").asString();
    }

    JsonNode dressed(String body) throws Exception {
        List<Error> requestErrors = REQUEST.validate(body, InputFormat.JSON);
        assertTrue(requestErrors.isEmpty(), "test request violates DressRequest: " + requestErrors + " in " + body);
        HttpResponse<String> res = post(body);
        assertEquals(200, res.statusCode(), body + " → " + res.body());
        List<Error> errors = DRESSED.validate(res.body(), InputFormat.JSON);
        assertTrue(errors.isEmpty(), "DressedAvatar violated: " + errors + " in " + res.body());
        return json.readTree(res.body());
    }

    static List<String> itemIds(JsonNode answer) {
        List<String> ids = new ArrayList<>();
        answer.get("layers").forEach(layer -> ids.add(layer.get("itemId").asString()));
        return ids;
    }

    static String item(String id, String size) {
        return "{\"id\": \"" + id + "\"" + (size == null ? "" : ", \"size\": " + size) + "}";
    }

    static String measurements(Measurements m) {
        return "{\"height\": " + m.height() + ", \"shoulders\": " + m.shoulders() + ", \"chest\": " + m.chest()
                + ", \"waist\": " + m.waist() + ", \"hips\": " + m.hips() + ", \"arm\": " + m.arm()
                + ", \"inseam\": " + m.inseam() + "}";
    }

    static String request(Measurements m, String... items) {
        return "{\"measurements\": " + measurements(m) + ", \"items\": [" + String.join(", ", items) + "]}";
    }

    HttpResponse<String> post(String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/avatar/dress"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> preflight(String origin) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/avatar/dress"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .build(), HttpResponse.BodyHandlers.ofString());
    }
}
