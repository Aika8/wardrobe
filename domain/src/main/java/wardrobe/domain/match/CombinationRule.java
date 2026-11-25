package wardrobe.domain.match;

import wardrobe.domain.Item;

/**
 * Pattern: Strategy — each way of judging whether two items go together is one rule; rules are combined later
 * (colour, formality, season). See docs/adr/0002-stack-and-hexagon.md.
 *
 * <p>Every rule is symmetric and an item (the same {@code ItemId}) goes with itself (intent 01, invariant 3).
 */
public interface CombinationRule {
    Verdict judge(Item a, Item b);
}
