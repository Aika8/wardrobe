package wardrobe.domain.avatar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import wardrobe.domain.ItemType;

/** Where a garment's edges fall on a flat figure. Measurements are invented. */
class PlacementTest {

    static final Measurements BODY = new Measurements(1650, 380, 880, 700, 960, 580, 760);
    static final Figure FIGURE = Figure.of(BODY);
    static final List<Measurements> EXTREMES = List.of(
            new Measurements(1300, 300, 600, 450, 650, 450, 1000),
            new Measurements(1300, 600, 1600, 1500, 1700, 800, 600),
            new Measurements(2200, 300, 600, 450, 650, 450, 600),
            new Measurements(2200, 600, 1600, 1500, 1700, 800, 1000),
            BODY);
    static final List<ItemType> WORN =
            List.of(ItemType.TOP, ItemType.BOTTOM, ItemType.DRESS, ItemType.OUTERWEAR, ItemType.SHOES);

    static Placement.Shape place(ItemType type, GarmentSize size) {
        return Placement.of(FIGURE, type, size);
    }

    @Test
    void eachTypeStartsAtItsLine() {
        assertEquals(FIGURE.shoulders(), place(ItemType.TOP, GarmentSize.empty()).top());
        assertEquals(FIGURE.shoulders(), place(ItemType.DRESS, GarmentSize.empty()).top());
        assertEquals(FIGURE.shoulders(), place(ItemType.OUTERWEAR, GarmentSize.empty()).top());
        assertEquals(FIGURE.waist(), place(ItemType.BOTTOM, GarmentSize.empty()).top());
        assertEquals(FIGURE.ankle(), place(ItemType.SHOES, GarmentSize.empty()).top());
    }

    @Test
    void withoutALengthEachTypeEndsAtItsDefault() {
        assertEquals(FIGURE.hips(), place(ItemType.TOP, GarmentSize.empty()).bottom());
        assertEquals(FIGURE.ankle(), place(ItemType.BOTTOM, GarmentSize.empty()).bottom());
        assertEquals(FIGURE.knee(), place(ItemType.DRESS, GarmentSize.empty()).bottom());
        assertEquals((FIGURE.crotch() + FIGURE.knee()) / 2, place(ItemType.OUTERWEAR, GarmentSize.empty()).bottom());
        assertEquals(FIGURE.floor(), place(ItemType.SHOES, GarmentSize.empty()).bottom());
    }

    @Test
    void midThighIsBetweenCrotchAndKnee() {
        int hem = place(ItemType.OUTERWEAR, GarmentSize.empty()).bottom();
        assertTrue(FIGURE.crotch() < hem && hem < FIGURE.knee());
    }

    @Test
    void aLengthIsMeasuredFromTheStart() {
        Placement.Shape top = place(ItemType.TOP, GarmentSize.empty().withLength(600));
        assertEquals(FIGURE.shoulders() + 600, top.bottom());
        Placement.Shape trousers = place(ItemType.BOTTOM, GarmentSize.empty().withLength(900));
        assertEquals(FIGURE.waist() + 900, trousers.bottom());
        assertFalse(top.clamped());
        assertFalse(trousers.clamped());
    }

    @Test
    void aLongerGarmentEndsLowerAndNeverAboveItsStart() {
        for (Measurements m : EXTREMES) {
            Figure figure = Figure.of(m);
            for (ItemType type : List.of(ItemType.TOP, ItemType.BOTTOM, ItemType.DRESS, ItemType.OUTERWEAR)) {
                int previous = Integer.MIN_VALUE;
                for (int length = 1; length <= 3000; length += 7) {
                    Placement.Shape s = Placement.of(figure, type, GarmentSize.empty().withLength(length));
                    assertTrue(s.bottom() > s.top(), type + " " + length + " ends above its start");
                    assertTrue(s.bottom() >= previous, type + " " + length + " ends higher than a shorter one");
                    assertTrue(s.bottom() <= figure.floor(), type + " " + length + " ends below the floor");
                    previous = s.bottom();
                }
            }
        }
    }

    @Test
    void noShapeEndsBelowTheFloorAndDefaultsAreNotClamped() {
        for (Measurements m : EXTREMES) {
            Figure figure = Figure.of(m);
            for (ItemType type : WORN) {
                Placement.Shape s = Placement.of(figure, type, GarmentSize.empty());
                assertTrue(s.top() < s.bottom(), type.toString());
                assertTrue(s.bottom() <= figure.floor(), type.toString());
                assertFalse(s.clamped(), type.toString());
            }
        }
    }

    @Test
    void aHemBelowTheFloorIsClampedAndSaysSo() {
        Placement.Shape gown = place(ItemType.DRESS, GarmentSize.empty().withLength(5000));
        assertEquals(FIGURE.floor(), gown.bottom());
        assertTrue(gown.clamped());
        Placement.Shape exact = place(ItemType.DRESS,
                GarmentSize.empty().withLength(FIGURE.floor() - FIGURE.shoulders()));
        assertEquals(FIGURE.floor(), exact.bottom());
        assertFalse(exact.clamped());
    }

    @Test
    void shoesIgnoreALengthFromTheSizeChart() {
        Placement.Shape shoes = place(ItemType.SHOES, GarmentSize.empty().withLength(240));
        assertEquals(FIGURE.ankle(), shoes.top());
        assertEquals(FIGURE.floor(), shoes.bottom());
        assertEquals(FIGURE.hipHalfWidth(), shoes.topHalfWidth());
        assertEquals(OptionalInt.empty(), shoes.sleeveEnd());
    }

    @Test
    void widthComesFromTheGarmentGirthWhenGiven() {
        Placement.Shape top = place(ItemType.TOP, GarmentSize.empty().withChest(1000).withHips(1100));
        assertEquals(Figure.halfWidthOfGirth(1000), top.topHalfWidth());
        assertEquals(Figure.halfWidthOfGirth(1100), top.bottomHalfWidth());
        Placement.Shape skirt = place(ItemType.BOTTOM, GarmentSize.empty().withWaist(760).withLength(100));
        assertEquals(Figure.halfWidthOfGirth(760), skirt.topHalfWidth());
        assertEquals(Figure.halfWidthOfGirth(760), skirt.bottomHalfWidth());
    }

    @Test
    void withoutAGirthWidthIsTheFigurePlusTheMinimumEase() {
        Placement.Shape top = place(ItemType.TOP, GarmentSize.empty());
        assertEquals(FIGURE.chestHalfWidth() + Figure.halfWidthOfGirth(Fit.MIN_EASE_CHEST), top.topHalfWidth());
        assertEquals(FIGURE.hipHalfWidth() + Figure.halfWidthOfGirth(Fit.MIN_EASE_HIPS), top.bottomHalfWidth());
        Placement.Shape trousers = place(ItemType.BOTTOM, GarmentSize.empty());
        assertEquals(FIGURE.waistHalfWidth() + Figure.halfWidthOfGirth(Fit.MIN_EASE_WAIST), trousers.topHalfWidth());
    }

    @Test
    void theEasedWidthIsWithinAMillimetreOfReadingTheEasedGirth() {
        for (Measurements m : EXTREMES) {
            Placement.Shape s = Placement.of(Figure.of(m), ItemType.TOP, GarmentSize.empty());
            int exact = Figure.halfWidthOfGirth(m.chest() + Fit.MIN_EASE_CHEST);
            assertTrue(Math.abs(s.topHalfWidth() - exact) <= 1, m.toString());
        }
    }

    @Test
    void sleeveEndsAtTheShoulderPlusTheSleeveForUpperGarments() {
        GarmentSize size = GarmentSize.empty().withSleeve(590);
        for (ItemType type : List.of(ItemType.TOP, ItemType.DRESS, ItemType.OUTERWEAR)) {
            assertEquals(OptionalInt.of(FIGURE.shoulders() + 590), place(type, size).sleeveEnd(), type.toString());
        }
        assertEquals(OptionalInt.empty(), place(ItemType.BOTTOM, size).sleeveEnd());
    }

    @Test
    void anAbsentSleeveStaysUnknown() {
        assertEquals(OptionalInt.empty(), place(ItemType.TOP, GarmentSize.empty()).sleeveEnd());
    }

    @Test
    void aSleeveBelowTheFloorIsClamped() {
        Placement.Shape s = place(ItemType.TOP, GarmentSize.empty().withSleeve(3000));
        assertEquals(OptionalInt.of(FIGURE.floor()), s.sleeveEnd());
        assertTrue(s.clamped());
        assertEquals(FIGURE.hips(), s.bottom());
    }

    @Test
    void anAccessoryHasNoPlacement() {
        assertThrows(IllegalArgumentException.class, () -> place(ItemType.ACCESSORY, GarmentSize.empty()));
    }

    @Test
    void theSameInputsGiveTheSameShape() {
        GarmentSize size = GarmentSize.empty().withChest(950).withLength(700).withSleeve(600);
        assertEquals(place(ItemType.OUTERWEAR, size), place(ItemType.OUTERWEAR, size));
    }
}
