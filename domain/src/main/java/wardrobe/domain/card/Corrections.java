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
 * The fields Aika set by hand while confirming a {@link CardDraft}; each one wins over the draft. An empty
 * {@code Optional} or an empty seasons set means "not set". A blank name, or {@code UNKNOWN} for colour, pattern or
 * fabric, is not a value a person supplies and counts as not set.
 */
public record Corrections(
        Optional<String> name,
        Optional<ItemType> type,
        Optional<Colour> colour,
        Optional<Pattern> pattern,
        Optional<Fabric> fabric,
        Set<Season> seasons,
        Optional<Formality> formality) {

    public Corrections {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(fabric, "fabric");
        Objects.requireNonNull(seasons, "seasons");
        Objects.requireNonNull(formality, "formality");
        name = name.map(String::trim).filter(n -> !n.isEmpty());
        colour = colour.filter(c -> c != Colour.UNKNOWN);
        pattern = pattern.filter(p -> p != Pattern.UNKNOWN);
        fabric = fabric.filter(f -> f != Fabric.UNKNOWN);
        seasons = seasons.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(seasons));
    }

    /** No field set by hand. */
    public static Corrections none() {
        return new Corrections(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Set.of(), Optional.empty());
    }

    public Corrections withName(String value) {
        return new Corrections(Optional.of(value), type, colour, pattern, fabric, seasons, formality);
    }

    public Corrections withType(ItemType value) {
        return new Corrections(name, Optional.of(value), colour, pattern, fabric, seasons, formality);
    }

    public Corrections withColour(Colour value) {
        return new Corrections(name, type, Optional.of(value), pattern, fabric, seasons, formality);
    }

    public Corrections withPattern(Pattern value) {
        return new Corrections(name, type, colour, Optional.of(value), fabric, seasons, formality);
    }

    public Corrections withFabric(Fabric value) {
        return new Corrections(name, type, colour, pattern, Optional.of(value), seasons, formality);
    }

    public Corrections withSeasons(Set<Season> value) {
        return new Corrections(name, type, colour, pattern, fabric, value, formality);
    }

    public Corrections withFormality(Formality value) {
        return new Corrections(name, type, colour, pattern, fabric, seasons, Optional.of(value));
    }
}
