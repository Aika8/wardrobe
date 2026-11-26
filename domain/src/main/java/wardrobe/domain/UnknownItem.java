package wardrobe.domain;

/** The catalogue has no item with this id. */
public final class UnknownItem extends RuntimeException {
    private final ItemId id;

    public UnknownItem(ItemId id) {
        super("no item " + id);
        this.id = id;
    }

    public ItemId id() {
        return id;
    }
}
