package wardrobe.domain.occasion;

import java.util.List;
import java.util.Objects;
import wardrobe.domain.ItemId;

/**
 * Whether a set of items suits an occasion. {@code items} are the items outside the occasion's range, in input
 * order (empty when it fits); {@code reason} is never blank.
 */
public record OccasionVerdict(boolean goes, Code code, List<ItemId> items, String reason) {

    /** Names go into {@code api/openapi.yaml} (WR-049): do not rename. */
    public enum Code {
        FITS_OCCASION,
        TOO_CASUAL,
        TOO_FORMAL
    }

    public OccasionVerdict {
        Objects.requireNonNull(code, "code");
        items = List.copyOf(items);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        if (goes != (code == Code.FITS_OCCASION)) {
            throw new IllegalArgumentException("goes=" + goes + " does not agree with code " + code);
        }
    }
}
