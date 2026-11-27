package wardrobe.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;
import wardrobe.domain.UnknownItem;

/**
 * Intent 01, invariant 9: there is no limit on the number of items. Over real HTTP against a catalogue of 2 000
 * invented items (generated here; no photos, no brands), {@code GET /items} returns every one of them in catalogue
 * order, and {@code GET /items?status=CLEAN} returns exactly the clean ones. A cap anywhere on the way fails this test.
 * Read-only.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoLimitApiTest {

    static final int COUNT = 2_000;

    /** 2 000 items cycling through the types, colours and the four statuses, so every status is well represented. */
    static final List<Item> MANY = generate();

    static List<Item> generate() {
        ItemType[] types = ItemType.values();
        Colour[] colours = Colour.values();
        ItemStatus[] statuses = ItemStatus.values();
        List<Item> items = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            items.add(new Item(new ItemId("item-" + i), "Invented item " + i, types[i % types.length],
                    colours[i % colours.length], Pattern.PLAIN, Fabric.COTTON, Set.of(Season.SUMMER),
                    Formality.CASUAL, statuses[i % statuses.length]));
        }
        return List.copyOf(items);
    }

    @TestConfiguration
    static class ManyItems {
        @Bean
        @Primary
        ItemCatalog manyItemsCatalog() {
            return new ItemCatalog() {
                @Override
                public List<Item> all() {
                    return MANY;
                }

                @Override
                public Optional<Item> find(ItemId id) {
                    return MANY.stream().filter(item -> item.id().equals(id)).findFirst();
                }

                @Override
                public void replace(Item item) {
                    throw new UnknownItem(item.id()); // read-only: this test never changes a status
                }
            };
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    final HttpClient http = HttpClient.newHttpClient();

    @Test
    void everyItemComesBack() throws Exception {
        List<String> ids = ids(get("/items"));

        assertEquals(COUNT, ids.size());
        assertEquals(MANY.stream().map(item -> item.id().value()).toList(), ids);
    }

    @Test
    void theCleanFilterReturnsExactlyTheCleanOnes() throws Exception {
        List<String> expected = MANY.stream()
                .filter(item -> item.status() == ItemStatus.CLEAN)
                .map(item -> item.id().value()).toList();

        List<String> ids = ids(get("/items?status=CLEAN"));

        assertEquals(COUNT / ItemStatus.values().length, expected.size(), "the generator spreads the statuses evenly");
        assertEquals(expected, ids);
    }

    JsonNode get(String path) throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    static List<String> ids(JsonNode list) {
        List<String> ids = new ArrayList<>();
        for (JsonNode item : list) {
            ids.add(item.get("id").asString());
        }
        return ids;
    }
}
