package wardrobe.domain.match;

import java.util.Objects;

/** The answer of a combination rule: whether two items go together, and why, in words a person reads. */
public record Verdict(boolean goes, String reason) {
    public Verdict {
        Objects.requireNonNull(reason, "reason");
    }

    public static Verdict yes(String reason) {
        return new Verdict(true, reason);
    }

    public static Verdict no(String reason) {
        return new Verdict(false, reason);
    }
}
