package wardrobe.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** WR-037: every fabric says whether it is light, medium or heavy; UNKNOWN stays unknown (intent 01, invariant 5). */
class FabricWeightTest {

    static final Map<Fabric, FabricWeight> EXPECTED_WEIGHT = new EnumMap<>(Map.ofEntries(
            Map.entry(Fabric.COTTON, FabricWeight.LIGHT),
            Map.entry(Fabric.LINEN, FabricWeight.LIGHT),
            Map.entry(Fabric.SILK, FabricWeight.LIGHT),
            Map.entry(Fabric.DENIM, FabricWeight.MEDIUM),
            Map.entry(Fabric.KNIT, FabricWeight.MEDIUM),
            Map.entry(Fabric.SYNTHETIC, FabricWeight.MEDIUM),
            Map.entry(Fabric.WOOL, FabricWeight.HEAVY),
            Map.entry(Fabric.LEATHER, FabricWeight.HEAVY),
            Map.entry(Fabric.UNKNOWN, FabricWeight.UNKNOWN)));

    @Test
    void everyFabricHasAnExpectedWeightInTheTable() {
        for (Fabric fabric : Fabric.values()) {
            assertTrue(EXPECTED_WEIGHT.containsKey(fabric), fabric + " is missing from the test table");
        }
    }

    @Test
    void everyFabricWeighsExactlyAsTheTableSays() {
        for (Fabric fabric : Fabric.values()) {
            assertEquals(EXPECTED_WEIGHT.get(fabric), fabric.weight(), fabric.name());
        }
    }

    @Test
    void unknownFabricHasUnknownWeight() {
        assertEquals(FabricWeight.UNKNOWN, Fabric.UNKNOWN.weight());
        assertFalse(Fabric.UNKNOWN.weight().known());
    }

    @Test
    void onlyUnknownWeightIsNotKnown() {
        for (FabricWeight weight : FabricWeight.values()) {
            assertEquals(weight != FabricWeight.UNKNOWN, weight.known(), weight.name());
        }
    }

    @Test
    void namesAndOrderOfFabricsStayAsTheJsonUsesThem() {
        assertEquals(
                "[COTTON, LINEN, WOOL, SILK, DENIM, LEATHER, KNIT, SYNTHETIC, UNKNOWN]",
                java.util.Arrays.toString(Fabric.values()));
    }
}
