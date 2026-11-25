package wardrobe.domain;

import java.util.List;

/** Port: where the items are kept (ADR-0002). Version 1 has one in-memory adapter in {@code app}. */
public interface ItemCatalog {
    /** All items, in a stable order. */
    List<Item> all();
}
