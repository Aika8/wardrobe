package wardrobe.domain;

import java.util.List;
import java.util.Optional;

/** Port: where the items are kept (ADR-0002). Version 1 has one in-memory adapter in {@code app}. */
public interface ItemCatalog {
    /** All items, in a stable order. */
    List<Item> all();

    /** The item with this id, or empty when the catalogue has none. */
    Optional<Item> find(ItemId id);

    /**
     * Puts {@code item} in place of the stored item with the same id, keeping its position in {@link #all()}.
     * Throws {@link UnknownItem} when no item has that id; the catalogue is then unchanged.
     */
    void replace(Item item);
}
