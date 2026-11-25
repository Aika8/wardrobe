package wardrobe.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static wardrobe.domain.ItemStatus.CLEAN;
import static wardrobe.domain.ItemStatus.IN_REPAIR;
import static wardrobe.domain.ItemStatus.IN_WASH;
import static wardrobe.domain.ItemStatus.ON_TRIP;
import static wardrobe.domain.TestItems.item;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Intent 01, invariants 1 and 2: one status, allowed changes only, only clean items go into outfits. */
class ItemStatusTest {

    static final Map<ItemStatus, Set<ItemStatus>> ALLOWED = Map.of(
            CLEAN, EnumSet.of(IN_WASH, IN_REPAIR, ON_TRIP),
            IN_WASH, EnumSet.of(CLEAN, IN_REPAIR),
            IN_REPAIR, EnumSet.of(CLEAN, IN_WASH),
            ON_TRIP, EnumSet.of(CLEAN, IN_WASH));

    @Test
    void everyPairOfStatusesFollowsTheTable() {
        for (ItemStatus from : ItemStatus.values()) {
            for (ItemStatus to : ItemStatus.values()) {
                Item before = item("x", from);
                if (ALLOWED.get(from).contains(to)) {
                    assertEquals(to, before.withStatus(to).status(), from + " -> " + to);
                } else {
                    assertThrows(IllegalStatusChange.class, () -> before.withStatus(to), from + " -> " + to);
                }
            }
        }
    }

    @Test
    void aRejectedChangeLeavesTheItemAsItWas() {
        Item inWash = item("x", IN_WASH);
        assertThrows(IllegalStatusChange.class, () -> inWash.withStatus(ON_TRIP));
        assertEquals(IN_WASH, inWash.status());
    }

    @Test
    void stayingInTheSameStatusIsNotAChange() {
        for (ItemStatus s : ItemStatus.values()) {
            assertFalse(s.canBecome(s), s.name());
        }
    }

    @Test
    void onlyCleanItemsAreAvailableForAnOutfit() {
        assertTrue(item("x", CLEAN).availableForOutfit());
        for (ItemStatus s : EnumSet.of(IN_WASH, IN_REPAIR, ON_TRIP)) {
            assertFalse(item("x", s).availableForOutfit(), s.name());
        }
    }
}
