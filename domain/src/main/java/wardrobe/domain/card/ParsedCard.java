package wardrobe.domain.card;

import java.util.List;
import java.util.Objects;

/**
 * What {@link CardParser#read} makes of a model answer: the card draft and one {@link Doubt} per field it left
 * unknown, in the key order of {@code docs/cards.md}. A field with no doubt was read clearly.
 */
public record ParsedCard(CardDraft draft, List<Doubt> doubts) {

    public ParsedCard {
        Objects.requireNonNull(draft, "draft");
        doubts = List.copyOf(doubts);
    }
}
