package wardrobe.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemTest {

    Item make(String name, Set<Season> seasons) {
        return new Item(new ItemId("a"), name, ItemType.BOTTOM, Colour.NAVY, Pattern.PLAIN, Fabric.DENIM, seasons,
                Formality.CASUAL, ItemStatus.CLEAN);
    }

    @Test
    void aCardNeedsANameAndASeason() {
        assertThrows(IllegalArgumentException.class, () -> make(" ", Set.of(Season.WINTER)));
        assertThrows(IllegalArgumentException.class, () -> make("jeans", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new ItemId(""));
    }

    @Test
    void seasonsAreCopiedSoTheCardCannotChangeFromOutside() {
        Set<Season> seasons = new HashSet<>(Set.of(Season.SPRING));
        Item item = make("jeans", seasons);
        seasons.add(Season.WINTER);
        assertEquals(Set.of(Season.SPRING), item.seasons());
        assertThrows(UnsupportedOperationException.class, () -> item.seasons().add(Season.SUMMER));
    }

    @Test
    void aStatusChangeKeepsTheCard() {
        Item item = make("jeans", EnumSet.of(Season.SPRING, Season.AUTUMN));
        Item washed = item.withStatus(ItemStatus.IN_WASH);
        assertEquals(item.id(), washed.id());
        assertEquals(item.seasons(), washed.seasons());
        assertTrue(washed.status() == ItemStatus.IN_WASH);
    }
}
