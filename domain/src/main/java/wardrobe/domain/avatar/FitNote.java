package wardrobe.domain.avatar;

import java.util.Objects;

/** One way a garment does not fit a figure: what is wrong ({@link Kind}) and where ({@link Part}). */
public record FitNote(Kind kind, Part part) {

    public enum Kind {
        /** Garment girth is less than body girth plus the minimum ease. */
        TOO_TIGHT,
        /** Garment girth is more than body girth plus the maximum ease. */
        TOO_LOOSE,
        SLEEVE_SHORT,
        SLEEVE_LONG,
        LEG_SHORT,
        LEG_LONG
    }

    /** The measured place of the garment the note is about. */
    public enum Part {
        CHEST,
        WAIST,
        HIPS,
        SLEEVE,
        INSEAM
    }

    public FitNote {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(part, "part");
    }
}
