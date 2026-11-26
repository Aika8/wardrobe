package wardrobe.domain.avatar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import wardrobe.domain.Item;
import wardrobe.domain.ItemType;
import wardrobe.domain.match.CombinationRule;
import wardrobe.domain.match.Verdict;
import wardrobe.domain.outfit.Outfit;
import wardrobe.domain.outfit.OutfitCheck;
import wardrobe.domain.outfit.OutfitProblem;

/**
 * The order in which the items of an outfit are drawn on the avatar, bottom layer first: SHOES, BOTTOM, TOP or
 * DRESS, OUTERWEAR, ACCESSORY; within one type, input order. Draft — вид проверяет Aika.
 *
 * <p>An outfit that cannot be put on a body ({@link OutfitProblem.TwoOfAKind}, {@link OutfitProblem.DressWithSeparates},
 * as {@link OutfitCheck} decides) is refused. Clashes and items that are not clean are drawn: the person may want
 * to see them. An empty outfit gives an empty order.
 */
public final class Layers {

    /** Structural problems do not depend on the combination rule; this one lets every pair go. */
    private static final CombinationRule ANY_PAIR = (a, b) -> Verdict.yes("not judged for drawing");

    private Layers() {
    }

    /** The items in drawing order; throws {@link IllegalArgumentException} listing the problems of an impossible outfit. */
    public static List<Item> order(Outfit outfit) {
        Objects.requireNonNull(outfit, "outfit");
        List<String> refused = OutfitCheck.check(outfit, ANY_PAIR).stream()
                .filter(p -> p instanceof OutfitProblem.TwoOfAKind || p instanceof OutfitProblem.DressWithSeparates)
                .map(OutfitProblem::message)
                .toList();
        if (!refused.isEmpty()) {
            throw new IllegalArgumentException("the avatar does not draw this outfit: " + String.join("; ", refused));
        }
        List<Item> ordered = new ArrayList<>(outfit.items());
        ordered.sort(Comparator.comparingInt(item -> layer(item.type())));
        return List.copyOf(ordered);
    }

    /** The layer of a type, 0 at the bottom. TOP and DRESS share a layer: an outfit never has both. */
    static int layer(ItemType type) {
        return switch (type) {
            case SHOES -> 0;
            case BOTTOM -> 1;
            case TOP, DRESS -> 2;
            case OUTERWEAR -> 3;
            case ACCESSORY -> 4;
        };
    }
}
