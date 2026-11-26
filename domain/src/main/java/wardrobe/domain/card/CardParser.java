package wardrobe.domain.card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/**
 * Reads a model answer in the card format of {@code docs/cards.md} into a {@link CardDraft}: one {@code key: value}
 * per line, keys and values case-insensitive, whitespace trimmed, enum words in English or in the Russian of
 * {@link RussianWords}.
 *
 * <p>A value that is not a word of its list, a key that is missing, or a key given twice with different values
 * leaves the field unknown — never guessed, never "the first one wins" (intent 01, invariant 5); {@link #read} says
 * which and why, as {@link Doubt}s (WR-014). Every line that
 * is not {@code key: value} with a key of the format is skipped: prose around the card, Markdown code fences, keys
 * we did not ask for ({@code brand:}, {@code size:}). Plain Java: no JSON library, no I/O.
 */
public final class CardParser {

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String COLOUR = "colour";
    private static final String PATTERN = "pattern";
    private static final String FABRIC = "fabric";
    private static final String SEASONS = "seasons";
    private static final String FORMALITY = "formality";
    private static final Set<String> KEYS = Set.of(NAME, TYPE, COLOUR, PATTERN, FABRIC, SEASONS, FORMALITY);

    private CardParser() {
    }

    /** The card the answer describes; fields the answer does not give clearly stay unknown. */
    public static CardDraft parse(String answer) {
        return read(answer).draft();
    }

    /**
     * The card the answer describes and, for each field left unknown, a {@link Doubt} naming the key, the raw value
     * and why: {@code MISSING} (no line or no value), {@code UNREADABLE} (not a word of the list) or
     * {@code CONFLICTING} (given twice differently). Doubts come in the key order of {@code docs/cards.md}.
     */
    public static ParsedCard read(String answer) {
        Map<String, List<String>> values = valuesByKey(answer == null ? "" : answer);
        List<Doubt> doubts = new ArrayList<>();
        Optional<ItemType> type = field(values, TYPE, word -> word(ItemType.class, word), doubts);
        Colour colour = field(values, COLOUR, word -> word(Colour.class, word), doubts).orElse(Colour.UNKNOWN);
        Pattern pattern = field(values, PATTERN, word -> word(Pattern.class, word), doubts).orElse(Pattern.UNKNOWN);
        Fabric fabric = field(values, FABRIC, word -> word(Fabric.class, word), doubts).orElse(Fabric.UNKNOWN);
        Set<Season> seasons = field(values, SEASONS, CardParser::seasons, doubts).orElse(Set.of());
        Optional<Formality> formality = field(values, FORMALITY, word -> word(Formality.class, word), doubts);
        Optional<String> name = field(values, NAME, Optional::of, doubts);
        return new ParsedCard(new CardDraft(name, type, colour, pattern, fabric, seasons, formality), doubts);
    }

    /** Every trimmed value per known key, in the order the answer gives them. */
    private static Map<String, List<String>> valuesByKey(String answer) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (String line : answer.split("\\R")) {
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).strip().toLowerCase(Locale.ROOT);
            if (KEYS.contains(key)) {
                values.computeIfAbsent(key, k -> new ArrayList<>()).add(line.substring(colon + 1).strip());
            }
        }
        return values;
    }

    /**
     * The key's value read by {@code reader}, if the answer gives it once (or several times alike, ignoring case)
     * and the reader understands it; otherwise empty, with the reason added to {@code doubts}.
     */
    private static <T> Optional<T> field(Map<String, List<String>> values, String key,
            Function<String, Optional<T>> reader, List<Doubt> doubts) {
        List<String> given = values.getOrDefault(key, List.of());
        if (given.isEmpty()) {
            doubts.add(new Doubt(key, "", Doubt.Why.MISSING));
            return Optional.empty();
        }
        String first = given.getFirst();
        if (!given.stream().allMatch(value -> value.equalsIgnoreCase(first))) {
            doubts.add(new Doubt(key, String.join(Doubt.SEPARATOR, given), Doubt.Why.CONFLICTING));
            return Optional.empty();
        }
        if (first.isEmpty()) {
            doubts.add(new Doubt(key, "", Doubt.Why.MISSING));
            return Optional.empty();
        }
        Optional<T> read = reader.apply(first);
        if (read.isEmpty()) {
            doubts.add(new Doubt(key, first, Doubt.Why.UNREADABLE));
        }
        return read;
    }

    /**
     * The enum constant named by the word, ignoring case ({@code smart_casual} names {@code SMART_CASUAL}), or by its
     * Russian word in {@link RussianWords}; a word in neither language is empty.
     */
    private static <E extends Enum<E>> Optional<E> word(Class<E> type, String word) {
        String wanted = word.strip().toUpperCase(Locale.ROOT);
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(wanted)) {
                return Optional.of(constant);
            }
        }
        return RussianWords.lookup(type, word);
    }

    /** Comma-separated seasons; if any word is not a season, the seasons are unknown (empty), not partly guessed. */
    private static Optional<Set<Season>> seasons(String value) {
        EnumSet<Season> seasons = EnumSet.noneOf(Season.class);
        for (String part : value.split(",")) {
            Optional<Season> season = word(Season.class, part);
            if (season.isEmpty()) {
                return Optional.empty();
            }
            seasons.add(season.get());
        }
        return Optional.of(seasons);
    }
}
