package wardrobe.domain.match;

import java.util.EnumSet;
import java.util.Set;
import wardrobe.domain.Item;
import wardrobe.domain.Season;

/**
 * Two items go together by season when they share at least one season. An item has at least one season (see
 * {@link Item}), so an item always goes with itself (intent 01, invariant 3). Seasons are named in enum order.
 */
public final class SeasonRule implements CombinationRule {

    @Override
    public Verdict judge(Item a, Item b) {
        EnumSet<Season> shared = EnumSet.copyOf(a.seasons());
        shared.retainAll(b.seasons());
        if (!shared.isEmpty()) {
            return Verdict.yes("both are worn in " + shared.iterator().next());
        }
        return Verdict.no("seasons " + inOrder(a.seasons()) + " and " + inOrder(b.seasons()) + " do not overlap");
    }

    private static String inOrder(Set<Season> seasons) {
        return EnumSet.copyOf(seasons).toString();
    }
}
