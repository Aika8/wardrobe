package wardrobe.domain.card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * leaves the field unknown — never guessed, never "the first one wins" (intent 01, invariant 5). Every line that
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
        Map<String, List<String>> values = valuesByKey(answer == null ? "" : answer);
        return new CardDraft(
                single(values, NAME).filter(name -> !name.isEmpty()),
                single(values, TYPE).flatMap(word -> word(ItemType.class, word)),
                single(values, COLOUR).flatMap(word -> word(Colour.class, word)).orElse(Colour.UNKNOWN),
                single(values, PATTERN).flatMap(word -> word(Pattern.class, word)).orElse(Pattern.UNKNOWN),
                single(values, FABRIC).flatMap(word -> word(Fabric.class, word)).orElse(Fabric.UNKNOWN),
                single(values, SEASONS).map(CardParser::seasons).orElse(Set.of()),
                single(values, FORMALITY).flatMap(word -> word(Formality.class, word)));
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

    /** The key's value if the answer gives it once, or several times alike (ignoring case); else empty. */
    private static Optional<String> single(Map<String, List<String>> values, String key) {
        List<String> given = values.getOrDefault(key, List.of());
        if (given.isEmpty()) {
            return Optional.empty();
        }
        String first = given.getFirst();
        boolean alike = given.stream().allMatch(value -> value.equalsIgnoreCase(first));
        return alike ? Optional.of(first) : Optional.empty();
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
    private static Set<Season> seasons(String value) {
        EnumSet<Season> seasons = EnumSet.noneOf(Season.class);
        for (String part : value.split(",")) {
            Optional<Season> season = word(Season.class, part);
            if (season.isEmpty()) {
                return Set.of();
            }
            seasons.add(season.get());
        }
        return seasons;
    }
}
