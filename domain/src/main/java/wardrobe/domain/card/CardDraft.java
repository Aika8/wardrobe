package wardrobe.domain.card;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/**
 * A card read from a model answer, before Aika confirms it. Any field may be unknown (intent 01, invariant 5):
 * {@code UNKNOWN} for colour, pattern and fabric; an empty {@code Optional} for name, type and formality (these
 * enums have no {@code UNKNOWN}); an empty set for seasons.
 */
public record CardDraft(
        Optional<String> name,
        Optional<ItemType> type,
        Colour colour,
        Pattern pattern,
        Fabric fabric,
        Set<Season> seasons,
        Optional<Formality> formality) {

    public CardDraft {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(fabric, "fabric");
        Objects.requireNonNull(seasons, "seasons");
        Objects.requireNonNull(formality, "formality");
        seasons = seasons.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(seasons));
    }

    /** A card with every field unknown. */
    public static CardDraft unknown() {
        return new CardDraft(Optional.empty(), Optional.empty(), Colour.UNKNOWN, Pattern.UNKNOWN, Fabric.UNKNOWN,
                Set.of(), Optional.empty());
    }
}
