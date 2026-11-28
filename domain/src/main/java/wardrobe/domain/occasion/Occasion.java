package wardrobe.domain.occasion;

import java.util.Objects;
import wardrobe.domain.Formality;

/**
 * What the day asks for, as a range of formality (both ends inclusive). The table is Aika's to tune (DECISIONS.md,
 * WR-040); {@link #allows(Formality)} is the only place it is read.
 */
public enum Occasion {
    OFFICE(Formality.SMART_CASUAL, Formality.BUSINESS),
    WEEKEND(Formality.CASUAL, Formality.SMART_CASUAL),
    EVENING(Formality.SMART_CASUAL, Formality.FORMAL),
    SPORT(Formality.SPORT, Formality.CASUAL);

    private final Formality min;
    private final Formality max;

    Occasion(Formality min, Formality max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(name() + ": min " + min + " is above max " + max);
        }
        this.min = min;
        this.max = max;
    }

    public Formality min() {
        return min;
    }

    public Formality max() {
        return max;
    }

    /** True when an item of this formality suits the occasion. */
    public boolean allows(Formality formality) {
        Objects.requireNonNull(formality, "formality");
        return formality.compareTo(min) >= 0 && formality.compareTo(max) <= 0;
    }
}
