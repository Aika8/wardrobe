package wardrobe.domain.avatar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import wardrobe.domain.ItemType;

/**
 * Does a garment of a given size fit a figure? Integer millimetres only. Girths compare the garment with the body
 * plus ease: below body + minimum ease is {@link FitNote.Kind#TOO_TIGHT}, above body + maximum ease is
 * {@link FitNote.Kind#TOO_LOOSE}; exactly at either limit fits. Sleeve is compared with the arm and inseam with the
 * inseam, each within ± a tolerance (exactly at the tolerance fits). Only the fields relevant to the item type are
 * checked; an absent field gives no note. Eases and tolerances are starting values for Aika to tune (DECISIONS.md,
 * WR-018) and live only here.
 */
public final class Fit {

    public static final int MIN_EASE_CHEST = 40;
    public static final int MIN_EASE_WAIST = 20;
    public static final int MIN_EASE_HIPS = 30;
    public static final int MAX_EASE = 200;
    public static final int SLEEVE_TOLERANCE = 30;
    public static final int INSEAM_TOLERANCE = 30;

    private static final Set<FitNote.Part> UPPER = Set.of(FitNote.Part.CHEST, FitNote.Part.WAIST, FitNote.Part.SLEEVE);
    private static final Set<FitNote.Part> LOWER = Set.of(FitNote.Part.WAIST, FitNote.Part.HIPS, FitNote.Part.INSEAM);
    private static final Set<FitNote.Part> WHOLE =
            Set.of(FitNote.Part.CHEST, FitNote.Part.WAIST, FitNote.Part.HIPS, FitNote.Part.SLEEVE);

    private Fit() {
    }

    /** The parts of a garment that the fit check looks at for an item type. */
    public static Set<FitNote.Part> relevantParts(ItemType type) {
        return switch (type) {
            case TOP -> UPPER;
            case BOTTOM -> LOWER;
            case DRESS, OUTERWEAR -> WHOLE;
            case SHOES, ACCESSORY -> Set.of();
        };
    }

    /** Fit notes for this garment on this figure, in part order (chest, waist, hips, sleeve, inseam); empty = fits. */
    public static List<FitNote> check(Measurements body, ItemType type, GarmentSize size) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(size, "size");
        Set<FitNote.Part> parts = relevantParts(type);
        List<FitNote> notes = new ArrayList<>();
        if (parts.contains(FitNote.Part.CHEST)) {
            girth(notes, FitNote.Part.CHEST, size.chest(), body.chest(), MIN_EASE_CHEST);
        }
        if (parts.contains(FitNote.Part.WAIST)) {
            girth(notes, FitNote.Part.WAIST, size.waist(), body.waist(), MIN_EASE_WAIST);
        }
        if (parts.contains(FitNote.Part.HIPS)) {
            girth(notes, FitNote.Part.HIPS, size.hips(), body.hips(), MIN_EASE_HIPS);
        }
        if (parts.contains(FitNote.Part.SLEEVE)) {
            length(notes, FitNote.Part.SLEEVE, size.sleeve(), body.arm(), SLEEVE_TOLERANCE,
                    FitNote.Kind.SLEEVE_SHORT, FitNote.Kind.SLEEVE_LONG);
        }
        if (parts.contains(FitNote.Part.INSEAM)) {
            length(notes, FitNote.Part.INSEAM, size.inseam(), body.inseam(), INSEAM_TOLERANCE,
                    FitNote.Kind.LEG_SHORT, FitNote.Kind.LEG_LONG);
        }
        return List.copyOf(notes);
    }

    private static void girth(List<FitNote> notes, FitNote.Part part, OptionalInt garment, int body, int minEase) {
        if (garment.isEmpty()) {
            return;
        }
        int g = garment.getAsInt();
        if (g < body + minEase) {
            notes.add(new FitNote(FitNote.Kind.TOO_TIGHT, part));
        } else if (g > body + MAX_EASE) {
            notes.add(new FitNote(FitNote.Kind.TOO_LOOSE, part));
        }
    }

    private static void length(List<FitNote> notes, FitNote.Part part, OptionalInt garment, int body, int tolerance,
            FitNote.Kind shortKind, FitNote.Kind longKind) {
        if (garment.isEmpty()) {
            return;
        }
        int g = garment.getAsInt();
        if (g < body - tolerance) {
            notes.add(new FitNote(shortKind, part));
        } else if (g > body + tolerance) {
            notes.add(new FitNote(longKind, part));
        }
    }
}
