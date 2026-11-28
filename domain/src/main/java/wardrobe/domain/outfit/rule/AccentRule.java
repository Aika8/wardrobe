package wardrobe.domain.outfit.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import wardrobe.domain.Colour;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.outfit.rule.WholeVerdict.Check;
import wardrobe.domain.outfit.rule.WholeVerdict.Code;

/**
 * An outfit carries at most {@link #MAX_ACCENTS} accent colours. An accent is a distinct non-neutral colour
 * ({@link Colour#neutral()}): one colour on two items counts once. An item of unknown colour makes the answer "no"
 * (invariant 5: an unknown colour is never guessed to be neutral).
 */
public final class AccentRule implements WholeRule {

    /** Aika's to tune (DECISIONS.md, WR-039). */
    public static final int MAX_ACCENTS = 2;

    /** The distinct non-neutral colours of these items in input order; {@code UNKNOWN} is not an accent. */
    public static List<Colour> accents(List<Item> items) {
        return List.copyOf(firstOfEachAccent(items).keySet());
    }

    @Override
    public WholeVerdict judge(List<Item> items) {
        List<ItemId> unknown = items.stream()
                .filter(item -> item.colour() == Colour.UNKNOWN)
                .map(Item::id)
                .toList();
        if (!unknown.isEmpty()) {
            return new WholeVerdict(Check.ACCENTS, false, Code.UNKNOWN_COLOUR, unknown,
                    "the colour of " + join(unknown.stream().map(ItemId::value).toList())
                            + " is not known, so the accents cannot be counted");
        }
        Map<Colour, ItemId> first = firstOfEachAccent(items);
        List<ItemId> ids = new ArrayList<>(first.values());
        String colours = join(first.keySet().stream().map(AccentRule::word).toList());
        if (first.isEmpty()) {
            return new WholeVerdict(Check.ACCENTS, true, Code.FEW_ACCENTS, ids, "all colours are neutral");
        }
        if (first.size() <= MAX_ACCENTS) {
            return new WholeVerdict(Check.ACCENTS, true, Code.FEW_ACCENTS, ids,
                    (first.size() == 1 ? "one accent colour: " : first.size() + " accent colours: ") + colours);
        }
        return new WholeVerdict(Check.ACCENTS, false, Code.TOO_MANY_ACCENTS, ids,
                first.size() + " accent colours (" + colours + "), more than " + MAX_ACCENTS);
    }

    private static Map<Colour, ItemId> firstOfEachAccent(List<Item> items) {
        Map<Colour, ItemId> first = new LinkedHashMap<>();
        for (Item item : items) {
            Colour colour = item.colour();
            if (colour != Colour.UNKNOWN && !colour.neutral()) {
                first.putIfAbsent(colour, item.id());
            }
        }
        return first;
    }

    private static String word(Colour colour) {
        return colour.name().toLowerCase(Locale.ROOT);
    }

    private static String join(List<String> words) {
        return words.stream().collect(Collectors.joining(", "));
    }
}
