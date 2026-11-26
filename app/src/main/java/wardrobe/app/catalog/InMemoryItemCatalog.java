package wardrobe.app.catalog;

import static wardrobe.domain.Season.AUTUMN;
import static wardrobe.domain.Season.SPRING;
import static wardrobe.domain.Season.SUMMER;
import static wardrobe.domain.Season.WINTER;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.UnknownItem;

/**
 * Version 1's only catalogue: a few invented items in memory (no photos, no real brands). Replaced by real storage
 * when Aika's own items come in — that is an ADR, not a quiet change.
 *
 * <p>Safe for concurrent requests: one lock guards an insertion-ordered map, so the seed order is kept and
 * {@link #all()} returns a snapshot that later changes do not touch.
 */
@Component
public class InMemoryItemCatalog implements ItemCatalog {

    static final List<Item> SEED = List.of(
            new Item(new ItemId("white-shirt"), "White shirt", ItemType.TOP, Colour.WHITE, Pattern.PLAIN,
                    Fabric.COTTON, Set.of(SPRING, SUMMER, AUTUMN), Formality.BUSINESS, ItemStatus.CLEAN),
            new Item(new ItemId("navy-trousers"), "Navy trousers", ItemType.BOTTOM, Colour.NAVY, Pattern.PLAIN,
                    Fabric.WOOL, Set.of(AUTUMN, WINTER, SPRING), Formality.BUSINESS, ItemStatus.CLEAN),
            new Item(new ItemId("blue-jeans"), "Blue jeans", ItemType.BOTTOM, Colour.BLUE, Pattern.PLAIN,
                    Fabric.DENIM, Set.of(SPRING, SUMMER, AUTUMN, WINTER), Formality.CASUAL, ItemStatus.IN_WASH),
            new Item(new ItemId("striped-tee"), "Striped T-shirt", ItemType.TOP, Colour.WHITE, Pattern.STRIPED,
                    Fabric.COTTON, Set.of(SUMMER), Formality.CASUAL, ItemStatus.CLEAN),
            new Item(new ItemId("beige-coat"), "Beige coat", ItemType.OUTERWEAR, Colour.BEIGE, Pattern.PLAIN,
                    Fabric.WOOL, Set.of(AUTUMN, WINTER), Formality.SMART_CASUAL, ItemStatus.IN_REPAIR),
            new Item(new ItemId("green-dress"), "Green dress", ItemType.DRESS, Colour.GREEN, Pattern.FLORAL,
                    Fabric.LINEN, Set.of(SUMMER), Formality.SMART_CASUAL, ItemStatus.ON_TRIP),
            new Item(new ItemId("white-sneakers"), "White sneakers", ItemType.SHOES, Colour.WHITE, Pattern.PLAIN,
                    Fabric.LEATHER, Set.of(SPRING, SUMMER, AUTUMN), Formality.SPORT, ItemStatus.CLEAN));

    private final Object lock = new Object();
    private final Map<ItemId, Item> items = new LinkedHashMap<>();

    public InMemoryItemCatalog() {
        this(SEED);
    }

    /** A catalogue holding exactly {@code seed}, in that order; ids must be unique. */
    InMemoryItemCatalog(List<Item> seed) {
        for (Item item : seed) {
            if (items.putIfAbsent(item.id(), item) != null) {
                throw new IllegalArgumentException("duplicate item id " + item.id());
            }
        }
    }

    @Override
    public List<Item> all() {
        synchronized (lock) {
            return List.copyOf(items.values());
        }
    }

    @Override
    public Optional<Item> find(ItemId id) {
        synchronized (lock) {
            return Optional.ofNullable(items.get(id));
        }
    }

    @Override
    public void replace(Item item) {
        synchronized (lock) {
            if (!items.containsKey(item.id())) {
                throw new UnknownItem(item.id());
            }
            items.put(item.id(), item); // an existing key keeps its place in a LinkedHashMap
        }
    }
}
