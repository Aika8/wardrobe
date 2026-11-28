package wardrobe.domain;

/**
 * How warm a fabric is, coarsely: one weight per fabric, not per item. {@code UNKNOWN} is for a fabric we do not know
 * (intent 01, invariant 5: never guessed).
 */
public enum FabricWeight {
    LIGHT,
    MEDIUM,
    HEAVY,
    UNKNOWN;

    /** Whether the weight is known; {@code false} only for {@code UNKNOWN}. */
    public boolean known() {
        return this != UNKNOWN;
    }
}
