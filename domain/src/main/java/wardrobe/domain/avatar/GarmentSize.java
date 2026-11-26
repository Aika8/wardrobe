package wardrobe.domain.avatar;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Sizes of a garment from its size chart, all integer millimetres (intent 01, invariant 6): chest, waist and hips
 * as girths of the garment; length; sleeve from shoulder seam to cuff; inseam. Any field may be absent, as a size
 * chart may lack it; an absent field is never guessed. A present value must be positive.
 */
public record GarmentSize(
        OptionalInt chest,
        OptionalInt waist,
        OptionalInt hips,
        OptionalInt length,
        OptionalInt sleeve,
        OptionalInt inseam) {

    private static final GarmentSize EMPTY = new GarmentSize(OptionalInt.empty(), OptionalInt.empty(),
            OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty());

    public GarmentSize {
        positive("chest", chest);
        positive("waist", waist);
        positive("hips", hips);
        positive("length", length);
        positive("sleeve", sleeve);
        positive("inseam", inseam);
    }

    /** A size with every field absent. */
    public static GarmentSize empty() {
        return EMPTY;
    }

    public GarmentSize withChest(int mm) {
        return new GarmentSize(OptionalInt.of(mm), waist, hips, length, sleeve, inseam);
    }

    public GarmentSize withWaist(int mm) {
        return new GarmentSize(chest, OptionalInt.of(mm), hips, length, sleeve, inseam);
    }

    public GarmentSize withHips(int mm) {
        return new GarmentSize(chest, waist, OptionalInt.of(mm), length, sleeve, inseam);
    }

    public GarmentSize withLength(int mm) {
        return new GarmentSize(chest, waist, hips, OptionalInt.of(mm), sleeve, inseam);
    }

    public GarmentSize withSleeve(int mm) {
        return new GarmentSize(chest, waist, hips, length, OptionalInt.of(mm), inseam);
    }

    public GarmentSize withInseam(int mm) {
        return new GarmentSize(chest, waist, hips, length, sleeve, OptionalInt.of(mm));
    }

    private static void positive(String field, OptionalInt value) {
        Objects.requireNonNull(value, field);
        if (value.isPresent() && value.getAsInt() <= 0) {
            throw new IllegalArgumentException(field + ": " + value.getAsInt() + " mm must be positive");
        }
    }
}
