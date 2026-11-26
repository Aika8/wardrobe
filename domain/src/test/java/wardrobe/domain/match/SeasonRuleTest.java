package wardrobe.domain.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static wardrobe.domain.match.SeasonItems.item;
import static wardrobe.domain.match.SeasonItems.nonEmptySeasonSets;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import wardrobe.domain.Item;
import wardrobe.domain.Season;

/** Intent 01, invariant 3; seasons are named in enum order (DECISIONS.md). */
class SeasonRuleTest {

    final SeasonRule rule = new SeasonRule();

    @Test
    void thereAreFifteenNonEmptySeasonSets() {
        assertEquals(15, nonEmptySeasonSets().size());
        assertEquals(15, Set.copyOf(nonEmptySeasonSets()).size());
    }

    @Test
    void overEveryPairOfSetsTheRuleIsSymmetricAndGoesExactlyWhenTheSetsIntersect() {
        List<Set<Season>> sets = nonEmptySeasonSets();
        int pairs = 0;
        for (Set<Season> sa : sets) {
            for (Set<Season> sb : sets) {
                Item a = item("a", sa);
                Item b = item("b", sb);
                EnumSet<Season> shared = EnumSet.copyOf(sa);
                shared.retainAll(sb);
                Verdict ab = rule.judge(a, b);
                Verdict ba = rule.judge(b, a);
                assertEquals(!shared.isEmpty(), ab.goes(), sa + " / " + sb);
                assertEquals(ab.goes(), ba.goes(), sa + " / " + sb);
                pairs++;
            }
        }
        assertEquals(15 * 15, pairs);
    }

    @Test
    void aYesNamesTheFirstSharedSeasonInEnumOrder() {
        List<Set<Season>> sets = nonEmptySeasonSets();
        for (Set<Season> sa : sets) {
            for (Set<Season> sb : sets) {
                EnumSet<Season> shared = EnumSet.copyOf(sa);
                shared.retainAll(sb);
                if (shared.isEmpty()) {
                    continue;
                }
                Season first = shared.iterator().next();
                Verdict v = rule.judge(item("a", sa), item("b", sb));
                assertTrue(v.reason().contains(first.name()), v.reason());
                for (Season other : Season.values()) {
                    if (other != first) {
                        assertFalse(v.reason().contains(other.name()), v.reason());
                    }
                }
            }
        }
        Verdict v = rule.judge(item("a", Set.of(Season.AUTUMN, Season.SPRING, Season.WINTER)),
                item("b", Set.of(Season.SPRING, Season.AUTUMN)));
        assertTrue(v.goes());
        assertTrue(v.reason().contains("SPRING") && !v.reason().contains("AUTUMN"), v.reason());
    }

    @Test
    void aNoNamesBothSetsInEnumOrder() {
        Verdict v = rule.judge(item("a", Set.of(Season.SUMMER, Season.WINTER)),
                item("b", Set.of(Season.AUTUMN, Season.SPRING)));
        assertFalse(v.goes());
        String reason = v.reason();
        assertTrue(reason.contains("[WINTER, SUMMER]"), reason);
        assertTrue(reason.contains("[SPRING, AUTUMN]"), reason);
        assertTrue(reason.indexOf("[WINTER, SUMMER]") < reason.indexOf("[SPRING, AUTUMN]"), reason);
    }

    @Test
    void everyItemGoesWithItself() {
        for (Set<Season> s : nonEmptySeasonSets()) {
            Item a = item("a", s);
            assertTrue(rule.judge(a, a).goes(), s.toString());
            assertTrue(rule.judge(a, item("a", s)).goes(), s + " (same id, another instance)");
        }
    }
}
