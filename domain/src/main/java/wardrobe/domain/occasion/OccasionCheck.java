package wardrobe.domain.occasion;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;

/**
 * Judges the formality of a whole set of items against an occasion. Every item counts, shoes and accessories
 * included. Any item below the range makes it TOO_CASUAL (even when another is too formal); otherwise any item
 * above makes it TOO_FORMAL. The range itself is read only through {@link Occasion#allows}.
 */
public final class OccasionCheck {

    private OccasionCheck() {
    }

    public static OccasionVerdict judge(List<Item> items, Occasion occasion) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(occasion, "occasion");
        if (items.isEmpty()) {
            return new OccasionVerdict(true, OccasionVerdict.Code.FITS_OCCASION, List.of(), "nothing to judge");
        }
        String range = occasion.min() + " and " + occasion.max();
        List<Item> outside = items.stream().filter(item -> !occasion.allows(item.formality())).toList();
        if (outside.isEmpty()) {
            return new OccasionVerdict(true, OccasionVerdict.Code.FITS_OCCASION, List.of(),
                    "everything is between " + range + ", as " + occasion + " asks");
        }
        boolean tooCasual = outside.stream().anyMatch(item -> item.formality().compareTo(occasion.min()) < 0);
        OccasionVerdict.Code code = tooCasual ? OccasionVerdict.Code.TOO_CASUAL : OccasionVerdict.Code.TOO_FORMAL;
        String named = outside.stream()
                .map(item -> item.name() + " (" + item.id() + ") is " + item.formality())
                .collect(Collectors.joining(", "));
        String reason = (tooCasual ? "too casual" : "too formal") + " for " + occasion + ", which asks for "
                + range + ": " + named;
        List<ItemId> ids = outside.stream().map(Item::id).toList();
        return new OccasionVerdict(false, code, ids, reason);
    }
}
