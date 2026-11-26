package wardrobe.app.web;

import java.util.List;
import wardrobe.domain.Item;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.Season;

/**
 * JSON shape of an item, as in {@code api/openapi.yaml} (schema {@code Item}). Seasons in calendar order; {@code next}
 * is {@link wardrobe.domain.ItemStatus#next()} in enum order, so the client never re-implements the status table.
 */
public record ItemView(String id, String name, String type, String colour, String pattern, String fabric,
                       List<String> seasons, String formality, String status, List<String> next) {

    static ItemView of(Item item) {
        return new ItemView(item.id().value(), item.name(), item.type().name(), item.colour().name(),
                item.pattern().name(), item.fabric().name(),
                item.seasons().stream().sorted().map(Season::name).toList(),
                item.formality().name(), item.status().name(),
                item.status().next().stream().sorted().map(ItemStatus::name).toList());
    }
}
