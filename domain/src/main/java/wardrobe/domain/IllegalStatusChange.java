package wardrobe.domain;

/** A status change the table in {@link ItemStatus#next()} does not allow (intent 01, invariant 2). */
public final class IllegalStatusChange extends RuntimeException {
    public IllegalStatusChange(ItemId id, ItemStatus from, ItemStatus to) {
        super("item " + id + " cannot go from " + from + " to " + to);
    }
}
