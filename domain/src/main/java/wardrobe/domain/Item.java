package wardrobe.domain;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * One item of clothing: its card (type, colour, pattern, fabric, seasons, formality) and its status. Immutable;
 * a status change returns a new item.
 */
public record Item(ItemId id, String name, ItemType type, Colour colour, Pattern pattern, Fabric fabric,
                   Set<Season> seasons, Formality formality, ItemStatus status) {

    public Item {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(fabric, "fabric");
        Objects.requireNonNull(formality, "formality");
        Objects.requireNonNull(status, "status");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("item " + id + ": name must not be blank");
        }
        if (seasons == null || seasons.isEmpty()) {
            throw new IllegalArgumentException("item " + id + ": at least one season");
        }
        seasons = Set.copyOf(EnumSet.copyOf(seasons));
    }

    /** The same item in another status; throws {@link IllegalStatusChange} when the change is not allowed. */
    public Item withStatus(ItemStatus target) {
        if (!status.canBecome(target)) {
            throw new IllegalStatusChange(id, status, target);
        }
        return new Item(id, name, type, colour, pattern, fabric, seasons, formality, target);
    }

    /** Only a clean item is offered for an outfit (intent 01, invariant 1). */
    public boolean availableForOutfit() {
        return status == ItemStatus.CLEAN;
    }
}
