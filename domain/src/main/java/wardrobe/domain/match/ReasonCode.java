package wardrobe.domain.match;

/**
 * Why a rule said yes or no, as a code a client can put in its own words without deciding anything again (one rule in
 * one place). The English {@link Verdict#reason()} says the same for a person reading the server's answer.
 */
public enum ReasonCode {
    /** The same item on both sides: an item goes with itself (intent 01, invariant 3). */
    SAME_ITEM,
    /** Formality at most {@link FormalityRule#MAX_STEPS} step apart. */
    CLOSE_ENOUGH,
    /** Formality more steps apart than allowed. */
    STEPS_APART,
    /** One of the colours is neutral. */
    NEUTRAL,
    /** Both items have the same colour. */
    SAME_COLOUR,
    /** The two colours are friends ({@link ColourRule#FRIENDS}). */
    FRIENDS,
    /** The two colours are not friends. */
    NOT_FRIENDS,
    /** A colour is unknown, so nothing is vouched for (intent 01, invariant 5). */
    UNKNOWN_COLOUR,
    /** At most one of the two items has a pattern. */
    ONE_PATTERN,
    /** Both items have a pattern (an unknown pattern counts as one). */
    TWO_PATTERNS,
    /** The items share a season. */
    SHARED_SEASON,
    /** The items share no season. */
    NO_SHARED_SEASON,
    /** Every item of the outfit is clean. */
    ALL_CLEAN,
    /** Some item of the outfit is not clean. */
    NOT_CLEAN,
    /** Fewer than two items: there is no pair to judge. */
    NO_PAIRS,
    /** A rule that gives no code (a composite or a rule written inline). */
    UNCODED
}
