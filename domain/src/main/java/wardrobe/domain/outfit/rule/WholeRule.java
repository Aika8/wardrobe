package wardrobe.domain.outfit.rule;

import java.util.List;
import wardrobe.domain.Item;

/**
 * Pattern: Strategy — one rule per way of judging a whole outfit. See docs/adr/0002-stack-and-hexagon.md.
 *
 * <p>Pair rules ({@code wardrobe.domain.match}) judge two items; a whole rule judges all items of an outfit at once.
 * The items come as a list so a caller can pass an outfit's items without a new type. Status is not judged here:
 * {@code OutfitCheck} does that (one rule in one place).
 */
public interface WholeRule {

    /** The verdict of this rule on these items; never {@code null}. */
    WholeVerdict judge(List<Item> items);
}
