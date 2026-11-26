package wardrobe.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A plain list behind the port, for use-case tests; counts the writes. */
final class FakeCatalog implements ItemCatalog {
    private final List<Item> items;
    int replaces;

    FakeCatalog(Item... items) {
        this.items = new ArrayList<>(List.of(items));
    }

    @Override
    public List<Item> all() {
        return List.copyOf(items);
    }

    @Override
    public Optional<Item> find(ItemId id) {
        return items.stream().filter(i -> i.id().equals(id)).findFirst();
    }

    @Override
    public void replace(Item item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id().equals(item.id())) {
                items.set(i, item);
                replaces++;
                return;
            }
        }
        throw new UnknownItem(item.id());
    }
}
