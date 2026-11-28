package wardrobe.domain.outfit.rule;

import java.util.List;
import java.util.Objects;
import wardrobe.domain.ItemId;

/**
 * The answer of a {@link WholeRule}: which check, whether the outfit passes it, why as a {@link Code} a client can put
 * in its own words and as words a person reads, and the items the answer is about.
 */
public record WholeVerdict(Check check, boolean goes, Code code, List<ItemId> items, String reason) {

    /** Which whole-outfit check gave the verdict. */
    public enum Check {
        ACCENTS, COMPLETE, ACCESSORIES
    }

    /** Why, as a stable code; these names are part of the API contract (WR-049): do not rename them. */
    public enum Code {
        FEW_ACCENTS,
        TOO_MANY_ACCENTS,
        UNKNOWN_COLOUR,
        COMPLETE,
        MISSING_BODY,
        MISSING_TOP,
        MISSING_BOTTOM,
        MISSING_SHOES,
        FEW_ACCESSORIES,
        TOO_MANY_ACCESSORIES
    }

    public WholeVerdict {
        Objects.requireNonNull(check, "check");
        Objects.requireNonNull(code, "code");
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("a whole verdict needs a reason (invariant 8)");
        }
    }
}
