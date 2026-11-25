package wardrobe.app.web;

import java.util.List;
import wardrobe.domain.Item;
import wardrobe.domain.Season;

/** JSON shape of an item, as in {@code api/openapi.yaml} (schema {@code Item}). Seasons in calendar order. */
public record ItemView(String id, String name, String type, String colour, String pattern, String fabric,
                       List<String> seasons, String formality, String status) {

    static ItemView of(Item item) {
        return new ItemView(item.id().value(), item.name(), item.type().name(), item.colour().name(),
                item.pattern().name(), item.fabric().name(),
                item.seasons().stream().sorted().map(Season::name).toList(),
                item.formality().name(), item.status().name());
    }
}
