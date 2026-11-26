package wardrobe.domain.card;

import java.util.Objects;

/**
 * Why one field of a {@link CardDraft} stayed unknown: the key of {@code docs/cards.md}, what the answer gave for it
 * as written (trimmed), and the reason. The raw value lets Aika see what the model said before she fills the field.
 *
 * @param key the card key in lower case ({@code colour}, {@code seasons}, …)
 * @param raw the value as the answer gave it: empty when {@link Why#MISSING}; every given value joined by
 *            {@code " | "} in answer order when {@link Why#CONFLICTING}
 * @param why the reason the field is unknown
 */
public record Doubt(String key, String raw, Why why) {

    /** The reason a field stayed unknown. */
    public enum Why {
        /** The answer has no line for the key, or the line has no value. */
        MISSING,
        /** The value is not a word of the key's list in English or Russian. */
        UNREADABLE,
        /** The key is given more than once with different values; none of them is taken. */
        CONFLICTING
    }

    /** Joins the values of a {@link Why#CONFLICTING} doubt. */
    public static final String SEPARATOR = " | ";

    public Doubt {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(why, "why");
    }
}
