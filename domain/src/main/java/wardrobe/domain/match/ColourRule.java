package wardrobe.domain.match;

import java.util.EnumSet;
import java.util.Set;
import wardrobe.domain.Colour;
import wardrobe.domain.Item;

/**
 * Two items go together by colour when either colour is neutral, both are the same colour, or the unordered pair is
 * one of {@link #FRIENDS}. An unknown colour is never vouched for (intent 01, invariant 5), except that an item always
 * goes with itself (invariant 3).
 */
public final class ColourRule implements CombinationRule {

    /** Unordered pairs of non-neutral colours that go together. Starting table; Aika revises it (DECISIONS.md). */
    static final Set<Set<Colour>> FRIENDS = Set.of(
            EnumSet.of(Colour.BLUE, Colour.YELLOW),
            EnumSet.of(Colour.BLUE, Colour.ORANGE),
            EnumSet.of(Colour.BLUE, Colour.PINK),
            EnumSet.of(Colour.GREEN, Colour.PINK),
            EnumSet.of(Colour.PURPLE, Colour.YELLOW),
            EnumSet.of(Colour.RED, Colour.PINK));

    @Override
    public Verdict judge(Item a, Item b) {
        if (a.id().equals(b.id())) {
            return Verdict.yes(ReasonCode.SAME_ITEM, "an item goes with itself");
        }
        if (a.colour() == Colour.UNKNOWN || b.colour() == Colour.UNKNOWN) {
            return Verdict.no(ReasonCode.UNKNOWN_COLOUR, "colour of " + unknownIds(a, b) + " is unknown");
        }
        if (a.colour().neutral() || b.colour().neutral()) {
            Colour neutral = a.colour().neutral() ? a.colour() : b.colour();
            return Verdict.yes(ReasonCode.NEUTRAL, neutral + " is neutral and goes with any colour");
        }
        if (a.colour() == b.colour()) {
            return Verdict.yes(ReasonCode.SAME_COLOUR, "both are " + a.colour());
        }
        if (FRIENDS.contains(EnumSet.of(a.colour(), b.colour()))) {
            return Verdict.yes(ReasonCode.FRIENDS, a.colour() + " and " + b.colour() + " go together");
        }
        return Verdict.no(ReasonCode.NOT_FRIENDS, a.colour() + " and " + b.colour() + " do not go together");
    }

    private static String unknownIds(Item a, Item b) {
        if (a.colour() == Colour.UNKNOWN && b.colour() == Colour.UNKNOWN) {
            return "items " + a.id() + " and " + b.id();
        }
        return "item " + (a.colour() == Colour.UNKNOWN ? a.id() : b.id());
    }
}
