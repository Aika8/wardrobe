package wardrobe.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** WR-001: every colour states whether it is neutral; UNKNOWN is not (intent 01, invariant 5). */
class ColourTest {

    static final Map<Colour, Boolean> EXPECTED_NEUTRAL = new EnumMap<>(Map.ofEntries(
            Map.entry(Colour.BLACK, true),
            Map.entry(Colour.WHITE, true),
            Map.entry(Colour.GREY, true),
            Map.entry(Colour.NAVY, true),
            Map.entry(Colour.BEIGE, true),
            Map.entry(Colour.BROWN, true),
            Map.entry(Colour.RED, false),
            Map.entry(Colour.PINK, false),
            Map.entry(Colour.ORANGE, false),
            Map.entry(Colour.YELLOW, false),
            Map.entry(Colour.GREEN, false),
            Map.entry(Colour.BLUE, false),
            Map.entry(Colour.PURPLE, false),
            Map.entry(Colour.UNKNOWN, false)));

    @Test
    void everyColourHasAnExpectedValueInTheTable() {
        for (Colour colour : Colour.values()) {
            assertTrue(EXPECTED_NEUTRAL.containsKey(colour), colour + " is missing from the test table");
        }
    }

    @Test
    void everyColourIsNeutralExactlyAsTheTableSays() {
        for (Colour colour : Colour.values()) {
            assertEquals(EXPECTED_NEUTRAL.get(colour), colour.neutral(), colour.name());
        }
    }

    @Test
    void unknownIsNotNeutral() {
        assertEquals(false, Colour.UNKNOWN.neutral());
    }
}
