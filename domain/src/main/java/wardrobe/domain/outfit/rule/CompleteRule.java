package wardrobe.domain.outfit.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemType;
import wardrobe.domain.outfit.rule.WholeVerdict.Check;
import wardrobe.domain.outfit.rule.WholeVerdict.Code;

/**
 * A complete outfit has a body (a TOP and a BOTTOM, or a DRESS) and SHOES. For suggestions and for a check the person
 * asks for; it does not change {@code OutfitCheck}, where a missing bottom is not a problem (WR-006).
 */
public final class CompleteRule implements WholeRule {

    @Override
    public WholeVerdict judge(List<Item> items) {
        Optional<Item> top = first(items, ItemType.TOP);
        Optional<Item> bottom = first(items, ItemType.BOTTOM);
        Optional<Item> dress = first(items, ItemType.DRESS);
        Optional<Item> shoes = first(items, ItemType.SHOES);

        List<Code> missing = new ArrayList<>();
        List<String> words = new ArrayList<>();
        if (dress.isEmpty()) {
            if (top.isEmpty() && bottom.isEmpty()) {
                missing.add(Code.MISSING_BODY);
                words.add("no top and bottom or dress");
            } else if (top.isEmpty()) {
                missing.add(Code.MISSING_TOP);
                words.add("no top");
            } else if (bottom.isEmpty()) {
                missing.add(Code.MISSING_BOTTOM);
                words.add("no bottom");
            }
        }
        if (shoes.isEmpty()) {
            missing.add(Code.MISSING_SHOES);
            words.add("no shoes");
        }
        if (!missing.isEmpty()) {
            return new WholeVerdict(Check.COMPLETE, false, missing.getFirst(), List.of(), String.join(" and ", words));
        }

        List<ItemId> body = new ArrayList<>();
        if (dress.isPresent()) {
            body.add(dress.get().id());
        } else {
            body.add(top.get().id());
            body.add(bottom.get().id());
        }
        body.add(shoes.get().id());
        return new WholeVerdict(Check.COMPLETE, true, Code.COMPLETE, body,
                dress.isPresent() ? "a dress and shoes" : "a top, a bottom and shoes");
    }

    private static Optional<Item> first(List<Item> items, ItemType type) {
        return items.stream().filter(item -> item.type() == type).findFirst();
    }
}
