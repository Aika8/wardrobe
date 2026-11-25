package wardrobe.domain;

/** Identity of an item; not blank. */
public record ItemId(String value) {
    public ItemId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("item id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
