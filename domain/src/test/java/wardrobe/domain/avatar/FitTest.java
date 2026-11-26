package wardrobe.domain.avatar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import wardrobe.domain.ItemType;
import wardrobe.domain.avatar.FitNote.Kind;
import wardrobe.domain.avatar.FitNote.Part;

class FitTest {

    private static final Measurements M = TestFigures.MEDIUM;

    /**
     * One limit of one note: at {@code limit} the garment fits; one millimetre past it ({@code limit + step}) the
     * note appears.
     */
    record Boundary(String name, ItemType type, int limit, int step, IntFunction<GarmentSize> size, FitNote note) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Boundary> boundaries() {
        GarmentSize e = GarmentSize.empty();
        return Stream.of(
                new Boundary("chest too tight", ItemType.TOP, M.chest() + Fit.MIN_EASE_CHEST, -1,
                        e::withChest, new FitNote(Kind.TOO_TIGHT, Part.CHEST)),
                new Boundary("chest too loose", ItemType.TOP, M.chest() + Fit.MAX_EASE, 1,
                        e::withChest, new FitNote(Kind.TOO_LOOSE, Part.CHEST)),
                new Boundary("waist too tight", ItemType.BOTTOM, M.waist() + Fit.MIN_EASE_WAIST, -1,
                        e::withWaist, new FitNote(Kind.TOO_TIGHT, Part.WAIST)),
                new Boundary("waist too loose", ItemType.BOTTOM, M.waist() + Fit.MAX_EASE, 1,
                        e::withWaist, new FitNote(Kind.TOO_LOOSE, Part.WAIST)),
                new Boundary("hips too tight", ItemType.BOTTOM, M.hips() + Fit.MIN_EASE_HIPS, -1,
                        e::withHips, new FitNote(Kind.TOO_TIGHT, Part.HIPS)),
                new Boundary("hips too loose", ItemType.BOTTOM, M.hips() + Fit.MAX_EASE, 1,
                        e::withHips, new FitNote(Kind.TOO_LOOSE, Part.HIPS)),
                new Boundary("sleeve short", ItemType.TOP, M.arm() - Fit.SLEEVE_TOLERANCE, -1,
                        e::withSleeve, new FitNote(Kind.SLEEVE_SHORT, Part.SLEEVE)),
                new Boundary("sleeve long", ItemType.TOP, M.arm() + Fit.SLEEVE_TOLERANCE, 1,
                        e::withSleeve, new FitNote(Kind.SLEEVE_LONG, Part.SLEEVE)),
                new Boundary("leg short", ItemType.BOTTOM, M.inseam() - Fit.INSEAM_TOLERANCE, -1,
                        e::withInseam, new FitNote(Kind.LEG_SHORT, Part.INSEAM)),
                new Boundary("leg long", ItemType.BOTTOM, M.inseam() + Fit.INSEAM_TOLERANCE, 1,
                        e::withInseam, new FitNote(Kind.LEG_LONG, Part.INSEAM)));
    }

    @ParameterizedTest
    @MethodSource("boundaries")
    void exactlyAtTheLimitFits(Boundary b) {
        assertEquals(List.of(), Fit.check(M, b.type(), b.size().apply(b.limit())));
    }

    @ParameterizedTest
    @MethodSource("boundaries")
    void oneMillimetrePastTheLimitGivesTheNote(Boundary b) {
        assertEquals(List.of(b.note()), Fit.check(M, b.type(), b.size().apply(b.limit() + b.step())));
    }

    @Test
    void startingValuesAsWrittenInDecisions() {
        assertEquals(40, Fit.MIN_EASE_CHEST);
        assertEquals(20, Fit.MIN_EASE_WAIST);
        assertEquals(30, Fit.MIN_EASE_HIPS);
        assertEquals(200, Fit.MAX_EASE);
        assertEquals(30, Fit.SLEEVE_TOLERANCE);
        assertEquals(30, Fit.INSEAM_TOLERANCE);
    }

    @ParameterizedTest
    @EnumSource(ItemType.class)
    void anEmptySizeGivesNoNote(ItemType type) {
        assertEquals(List.of(), Fit.check(M, type, GarmentSize.empty()));
    }

    /** Every field far too small: only the parts relevant to the type are noted. */
    private static final GarmentSize TINY = GarmentSize.empty()
            .withChest(100).withWaist(100).withHips(100).withLength(100).withSleeve(100).withInseam(100);

    @Test
    void topLooksAtChestWaistAndSleeve() {
        assertEquals(List.of(new FitNote(Kind.TOO_TIGHT, Part.CHEST), new FitNote(Kind.TOO_TIGHT, Part.WAIST),
                new FitNote(Kind.SLEEVE_SHORT, Part.SLEEVE)), Fit.check(M, ItemType.TOP, TINY));
    }

    @Test
    void bottomLooksAtWaistHipsAndInseam() {
        assertEquals(List.of(new FitNote(Kind.TOO_TIGHT, Part.WAIST), new FitNote(Kind.TOO_TIGHT, Part.HIPS),
                new FitNote(Kind.LEG_SHORT, Part.INSEAM)), Fit.check(M, ItemType.BOTTOM, TINY));
    }

    @ParameterizedTest
    @EnumSource(value = ItemType.class, names = {"DRESS", "OUTERWEAR"})
    void dressAndOuterwearLookAtChestWaistHipsAndSleeve(ItemType type) {
        assertEquals(List.of(new FitNote(Kind.TOO_TIGHT, Part.CHEST), new FitNote(Kind.TOO_TIGHT, Part.WAIST),
                new FitNote(Kind.TOO_TIGHT, Part.HIPS), new FitNote(Kind.SLEEVE_SHORT, Part.SLEEVE)),
                Fit.check(M, type, TINY));
    }

    @ParameterizedTest
    @EnumSource(value = ItemType.class, names = {"SHOES", "ACCESSORY"})
    void shoesAndAccessoriesAreNotChecked(ItemType type) {
        assertEquals(List.of(), Fit.check(M, type, TINY));
    }

    @Test
    void aSizeThatFitsGivesNoNote() {
        GarmentSize good = GarmentSize.empty().withChest(1000).withWaist(820).withHips(1080).withSleeve(575);
        assertEquals(List.of(), Fit.check(M, ItemType.DRESS, good));
    }

    @Test
    void aNonPositiveSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> GarmentSize.empty().withChest(0));
        assertThrows(IllegalArgumentException.class, () -> GarmentSize.empty().withInseam(-5));
    }
}
