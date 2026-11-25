package wardrobe.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where an item is now (intent 01, invariants 1 and 2). Only a clean item takes part in outfits. The allowed
 * changes are the table in {@link #next()}; anything else is rejected by {@link Item#withStatus(ItemStatus)}.
 */
public enum ItemStatus {
    CLEAN,
    IN_WASH,
    IN_REPAIR,
    ON_TRIP;

    /** Statuses this one may change to. Staying in the same status is not a change and is not listed. */
    public Set<ItemStatus> next() {
        return switch (this) {
            case CLEAN -> EnumSet.of(IN_WASH, IN_REPAIR, ON_TRIP);
            case IN_WASH -> EnumSet.of(CLEAN, IN_REPAIR);       // a hole found while washing
            case IN_REPAIR -> EnumSet.of(CLEAN, IN_WASH);
            case ON_TRIP -> EnumSet.of(CLEAN, IN_WASH);         // comes back clean or straight into the wash
        };
    }

    public boolean canBecome(ItemStatus target) {
        return next().contains(target);
    }
}
