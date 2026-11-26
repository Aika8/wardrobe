package wardrobe.domain.outfit;

import java.util.Objects;
import wardrobe.domain.Item;
import wardrobe.domain.ItemType;

/** One thing wrong with an outfit, with a {@link #message()} a person reads. */
public sealed interface OutfitProblem {

    String message();

    /** The item is not clean, so it is not offered for an outfit (intent 01, invariant 1). */
    record NotClean(Item item) implements OutfitProblem {
        public NotClean {
            Objects.requireNonNull(item, "item");
        }

        @Override
        public String message() {
            return item.name() + " is not clean (" + item.status() + ")";
        }
    }

    /** Two items of a type worn at most once (every type but ACCESSORY). */
    record TwoOfAKind(ItemType type, Item a, Item b) implements OutfitProblem {
        public TwoOfAKind {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(a, "a");
            Objects.requireNonNull(b, "b");
        }

        @Override
        public String message() {
            return a.name() + " and " + b.name() + " are both " + type + "; an outfit has one";
        }
    }

    /** A dress is worn instead of a top and a bottom, not with them. */
    record DressWithSeparates(Item dress, Item other) implements OutfitProblem {
        public DressWithSeparates {
            Objects.requireNonNull(dress, "dress");
            Objects.requireNonNull(other, "other");
        }

        @Override
        public String message() {
            return dress.name() + " is a dress and is not worn with " + other.name() + " (" + other.type() + ")";
        }
    }

    /** The combination rule says this pair does not go; {@code reason} is its verdict's reason. */
    record Clash(Item a, Item b, String reason) implements OutfitProblem {
        public Clash {
            Objects.requireNonNull(a, "a");
            Objects.requireNonNull(b, "b");
            Objects.requireNonNull(reason, "reason");
        }

        @Override
        public String message() {
            return a.name() + " and " + b.name() + " do not go: " + reason;
        }
    }

    /** No items at all. */
    record Empty() implements OutfitProblem {
        @Override
        public String message() {
            return "the outfit has no items";
        }
    }
}
