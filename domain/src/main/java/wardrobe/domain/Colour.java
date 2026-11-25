package wardrobe.domain;

/**
 * The main colour of an item, from a fixed palette (intent 01, open question: measured colours may come later).
 * {@code UNKNOWN} is for a card whose colour a model answer did not give (invariant 5: never guessed).
 *
 * <p>Each colour states whether it is neutral as a constructor argument, so a new colour cannot be added without
 * deciding. {@code UNKNOWN} is not neutral: we cannot vouch for a colour we do not know.
 */
public enum Colour {
    BLACK(true),
    WHITE(true),
    GREY(true),
    NAVY(true),
    BEIGE(true),
    BROWN(true),
    RED(false),
    PINK(false),
    ORANGE(false),
    YELLOW(false),
    GREEN(false),
    BLUE(false),
    PURPLE(false),
    UNKNOWN(false);

    private final boolean neutral;

    Colour(boolean neutral) {
        this.neutral = neutral;
    }

    /** Whether this colour is neutral (goes with any colour); {@code false} for {@code UNKNOWN}. */
    public boolean neutral() {
        return neutral;
    }
}
