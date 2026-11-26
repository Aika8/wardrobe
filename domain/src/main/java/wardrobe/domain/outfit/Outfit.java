package wardrobe.domain.outfit;

import java.util.List;
import java.util.Objects;
import wardrobe.domain.Item;

/** Items worn together, in the order they were picked. May be empty; {@link OutfitCheck} says what is wrong. */
public record Outfit(List<Item> items) {
    public Outfit {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }
}
