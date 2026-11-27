package wardrobe.domain.avatar;

import java.util.Objects;
import java.util.OptionalInt;
import wardrobe.domain.ItemType;

/**
 * Draft — вид проверяет Aika. Where a garment's edges fall on a flat {@link Figure}: top and bottom y, half-width at
 * each edge, and the sleeve end. Integer millimetres, y grows down from the top of the head, integer arithmetic only.
 *
 * <p>Start: TOP, DRESS and OUTERWEAR at the shoulder line, BOTTOM at the waist, SHOES at the ankle. Length: from the
 * size when it has one, measured from the start; else the default of {@link #defaultHem} (TOP to the hips, BOTTOM to
 * the ankle, DRESS to the knee, OUTERWEAR to mid-thigh). SHOES always go from the ankle to the floor: a shoe's
 * length on a size chart is the foot, not a height. A hem below the floor is put on the floor and the shape says
 * {@code clamped}.
 *
 * <p>Width at an edge: the girth of the body part at that height (above the waist line chest, above the hip line
 * waist, else hips) — the garment's own girth if the size gives it, else the body's plus the minimum ease of
 * {@link Fit} — read as a front width the way {@link Figure} reads a girth. SHOES are as wide as the hips of the
 * figure. A {@link Figure} keeps half-widths, not girths, so the body-plus-ease width is the body half-width plus
 * the ease read the same way (at most 1 mm off from reading the sum, by rounding). Sleeve end (TOP, DRESS,
 * OUTERWEAR): shoulder line plus the sleeve of the size, clamped to the floor like a hem; a size without a sleeve
 * leaves it unknown (empty), as the figure has no arm length. ACCESSORY has no placement.
 */
public final class Placement {

    private Placement() {
    }

    /** The edges of a garment on a figure, all mm; {@code sleeveEnd} is empty for types without sleeves. */
    public record Shape(int top, int bottom, int topHalfWidth, int bottomHalfWidth, OptionalInt sleeveEnd,
            boolean clamped) {
    }

    /** The shape of a garment of this type and size on this figure; throws for ACCESSORY. */
    public static Shape of(Figure figure, ItemType type, GarmentSize size) {
        Objects.requireNonNull(figure, "figure");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(size, "size");
        if (type == ItemType.ACCESSORY) {
            throw new IllegalArgumentException("an accessory has no placement on the figure");
        }
        int floor = figure.floor();
        if (type == ItemType.SHOES) {
            return new Shape(figure.ankle(), floor, figure.hipHalfWidth(), figure.hipHalfWidth(),
                    OptionalInt.empty(), false);
        }
        int top = start(figure, type);
        int hem = size.length().isPresent() ? top + size.length().getAsInt() : defaultHem(figure, type);
        boolean clamped = hem > floor;
        int bottom = Math.min(hem, floor);
        OptionalInt sleeveEnd = OptionalInt.empty();
        if (type != ItemType.BOTTOM && size.sleeve().isPresent()) {
            int end = top + size.sleeve().getAsInt();
            clamped |= end > floor;
            sleeveEnd = OptionalInt.of(Math.min(end, floor));
        }
        return new Shape(top, bottom, halfWidthAt(figure, size, top), halfWidthAt(figure, size, bottom), sleeveEnd,
                clamped);
    }

    /** Where a garment of this type starts. */
    static int start(Figure figure, ItemType type) {
        return switch (type) {
            case TOP, DRESS, OUTERWEAR -> figure.shoulders();
            case BOTTOM -> figure.waist();
            case SHOES -> figure.ankle();
            case ACCESSORY -> throw new IllegalArgumentException("an accessory has no placement on the figure");
        };
    }

    /** The one table of default hems, used when the size has no length. */
    static int defaultHem(Figure figure, ItemType type) {
        return switch (type) {
            case TOP -> figure.hips();
            case BOTTOM -> figure.ankle();
            case DRESS -> figure.knee();
            case OUTERWEAR -> (figure.crotch() + figure.knee()) / 2;
            case SHOES -> figure.floor();
            case ACCESSORY -> throw new IllegalArgumentException("an accessory has no placement on the figure");
        };
    }

    private static int halfWidthAt(Figure figure, GarmentSize size, int y) {
        if (y < figure.waist()) {
            return halfWidth(size.chest(), figure.chestHalfWidth(), Fit.MIN_EASE_CHEST);
        }
        if (y < figure.hips()) {
            return halfWidth(size.waist(), figure.waistHalfWidth(), Fit.MIN_EASE_WAIST);
        }
        return halfWidth(size.hips(), figure.hipHalfWidth(), Fit.MIN_EASE_HIPS);
    }

    /** The garment's girth read as a half-width; absent, the body's half-width plus the half-width of the ease. */
    private static int halfWidth(OptionalInt garmentGirth, int bodyHalfWidth, int minEase) {
        return garmentGirth.isPresent()
                ? Figure.halfWidthOfGirth(garmentGirth.getAsInt())
                : bodyHalfWidth + Figure.halfWidthOfGirth(minEase);
    }
}
