package wardrobe.domain;

/**
 * The main colour of an item, from a fixed palette (intent 01, open question: measured colours may come later).
 * {@code UNKNOWN} is for a card whose colour a model answer did not give (invariant 5: never guessed).
 */
public enum Colour {
    BLACK, WHITE, GREY, NAVY, BEIGE, BROWN, RED, PINK, ORANGE, YELLOW, GREEN, BLUE, PURPLE, UNKNOWN
}
