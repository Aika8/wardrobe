package wardrobe.domain.match;

import java.util.Objects;

/**
 * The answer of a combination rule: whether two items go together, why in words a person reads, and the same why as a
 * {@link ReasonCode} a client can put in its own words.
 */
public record Verdict(boolean goes, String reason, ReasonCode code) {
    public Verdict {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(code, "code");
    }

    /** A verdict without a code ({@link ReasonCode#UNCODED}). */
    public Verdict(boolean goes, String reason) {
        this(goes, reason, ReasonCode.UNCODED);
    }

    public static Verdict yes(String reason) {
        return new Verdict(true, reason);
    }

    public static Verdict no(String reason) {
        return new Verdict(false, reason);
    }

    public static Verdict yes(ReasonCode code, String reason) {
        return new Verdict(true, reason, code);
    }

    public static Verdict no(ReasonCode code, String reason) {
        return new Verdict(false, reason, code);
    }
}
