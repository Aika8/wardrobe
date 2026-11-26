package wardrobe.domain.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/**
 * Turns a {@link CardDraft} into an {@link Item} once Aika has looked at it. Her {@link Corrections} win over the
 * draft. A field that is still unknown after that is never guessed (intent 01, invariant 5): the card is not
 * confirmed and {@link IncompleteCard} names the fields. Version 1 does not confirm {@code UNKNOWN} colour, pattern
 * or fabric either: the combination rules cannot judge them (DECISIONS.md, WR-015).
 */
public final class ConfirmCard {

    private ConfirmCard() {
    }

    /** A new item in status {@link ItemStatus#CLEAN}; throws {@link IncompleteCard} when a field is unknown. */
    public static Item confirm(ItemId id, CardDraft draft, Corrections corrections) {
        Optional<ItemType> type = corrections.type().or(draft::type);
        Colour colour = corrections.colour().orElse(draft.colour());
        Pattern pattern = corrections.pattern().orElse(draft.pattern());
        Fabric fabric = corrections.fabric().orElse(draft.fabric());
        Set<Season> seasons = corrections.seasons().isEmpty() ? draft.seasons() : corrections.seasons();
        Optional<Formality> formality = corrections.formality().or(draft::formality);
        Optional<String> name = corrections.name().or(() -> draft.name().map(String::trim).filter(n -> !n.isEmpty()));

        List<String> unknown = new ArrayList<>();
        if (type.isEmpty()) {
            unknown.add("type");
        }
        if (colour == Colour.UNKNOWN) {
            unknown.add("colour");
        }
        if (pattern == Pattern.UNKNOWN) {
            unknown.add("pattern");
        }
        if (fabric == Fabric.UNKNOWN) {
            unknown.add("fabric");
        }
        if (seasons.isEmpty()) {
            unknown.add("seasons");
        }
        if (formality.isEmpty()) {
            unknown.add("formality");
        }
        if (name.isEmpty()) {
            unknown.add("name");
        }
        if (!unknown.isEmpty()) {
            throw new IncompleteCard(unknown);
        }
        return new Item(id, name.get(), type.get(), colour, pattern, fabric, seasons, formality.get(),
                ItemStatus.CLEAN);
    }
}
