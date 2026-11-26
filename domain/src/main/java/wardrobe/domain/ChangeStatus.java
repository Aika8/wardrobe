package wardrobe.domain;

import java.util.Objects;

/**
 * Use case: move one item to another status. Whether the change is allowed is decided by
 * {@link Item#withStatus(ItemStatus)} (the table in {@link ItemStatus#next()}); this class only finds the item and
 * stores the result, so an illegal change leaves the catalogue as it was.
 */
public final class ChangeStatus {
    private final ItemCatalog catalog;

    public ChangeStatus(ItemCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /**
     * The item in its new status, already stored. Throws {@link UnknownItem} for an id the catalogue lacks and
     * {@link IllegalStatusChange} for a change the status table does not allow.
     */
    public Item change(ItemId id, ItemStatus target) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(target, "target");
        Item current = catalog.find(id).orElseThrow(() -> new UnknownItem(id));
        Item changed = current.withStatus(target);
        catalog.replace(changed);
        return changed;
    }
}
