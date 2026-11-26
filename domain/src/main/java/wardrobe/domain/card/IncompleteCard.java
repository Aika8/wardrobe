package wardrobe.domain.card;

import java.util.List;

/**
 * A card cannot become an item: some fields are still unknown after Aika's corrections (intent 01, invariant 5).
 * {@link #keys()} lists them as lower-case card keys in the key order of {@code docs/cards.md}.
 */
public final class IncompleteCard extends RuntimeException {
    private final List<String> keys;

    public IncompleteCard(List<String> keys) {
        super("card is incomplete, still unknown: " + String.join(", ", keys));
        this.keys = List.copyOf(keys);
    }

    public List<String> keys() {
        return keys;
    }
}
