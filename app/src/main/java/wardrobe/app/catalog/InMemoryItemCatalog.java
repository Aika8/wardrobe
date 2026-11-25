package wardrobe.app.catalog;

import static wardrobe.domain.Season.AUTUMN;
import static wardrobe.domain.Season.SPRING;
import static wardrobe.domain.Season.SUMMER;
import static wardrobe.domain.Season.WINTER;

import java.util.List;
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

/**
 * Version 1's only catalogue: a few invented items in memory (no photos, no real brands). Replaced by real storage
 * when Aika's own items come in — that is an ADR, not a quiet change.
 */
@Component
public class InMemoryItemCatalog implements ItemCatalog {

    private final List<Item> items = List.of(
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

    @Override
    public List<Item> all() {
        return items;
    }
}
