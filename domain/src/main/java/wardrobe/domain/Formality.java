package wardrobe.domain;

/** How formal an item is, an ordered scale from sport to formal (intent 01, invariant 4). */
public enum Formality {
    SPORT, CASUAL, SMART_CASUAL, BUSINESS, FORMAL;

    /** Number of steps between two levels of the scale, never negative. */
    public int stepsTo(Formality other) {
        return Math.abs(ordinal() - other.ordinal());
    }
}
