package wardrobe.domain.outfit.rule;

import java.util.List;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemType;
import wardrobe.domain.outfit.rule.WholeVerdict.Check;
import wardrobe.domain.outfit.rule.WholeVerdict.Code;

/** An outfit carries at most {@link #MAX_ACCESSORIES} accessories. */
public final class AccessoryRule implements WholeRule {

    /** Aika's to tune (DECISIONS.md, WR-039). */
    public static final int MAX_ACCESSORIES = 3;

    @Override
    public WholeVerdict judge(List<Item> items) {
        List<ItemId> accessories = items.stream()
                .filter(item -> item.type() == ItemType.ACCESSORY)
                .map(Item::id)
                .toList();
        int n = accessories.size();
        if (n <= MAX_ACCESSORIES) {
            return new WholeVerdict(Check.ACCESSORIES, true, Code.FEW_ACCESSORIES, accessories,
                    n == 0 ? "no accessories" : n == 1 ? "one accessory" : n + " accessories");
        }
        return new WholeVerdict(Check.ACCESSORIES, false, Code.TOO_MANY_ACCESSORIES, accessories,
                n + " accessories, more than " + MAX_ACCESSORIES);
    }
}
