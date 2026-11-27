package wardrobe.domain.outfit;

import java.util.List;
import java.util.Objects;
import wardrobe.domain.ItemId;
import wardrobe.domain.match.ReasonCode;

/**
 * One line of an outfit's explanation: for one rule, whether the outfit passes it (✓ or ✗), the reason as a code the
 * client says in its own words, the ids of the items the line is about, and the reason in English.
 */
public record WhyLine(Rule rule, boolean goes, ReasonCode code, List<ItemId> items, String reason) {

    /** The rules an outfit is explained by, in the order the lines come. */
    public enum Rule {
        FORMALITY, COLOUR, PATTERN, SEASON, CLEAN
    }

    public WhyLine {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(reason, "reason");
        items = List.copyOf(items);
        if (reason.isBlank()) {
            throw new IllegalArgumentException("a why line needs a reason");
        }
    }
}
