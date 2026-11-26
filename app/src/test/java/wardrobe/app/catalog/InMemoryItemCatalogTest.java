package wardrobe.app.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import wardrobe.domain.ChangeStatus;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.IllegalStatusChange;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;
import wardrobe.domain.UnknownItem;

class InMemoryItemCatalogTest {

    static Item item(String id, ItemStatus status) {
        return new Item(new ItemId(id), id, ItemType.TOP, Colour.WHITE, Pattern.PLAIN, Fabric.COTTON,
                Set.of(Season.SUMMER), Formality.CASUAL, status);
    }

    static List<ItemId> ids(List<Item> items) {
        return items.stream().map(Item::id).toList();
    }

    @Test
    void theSeedItemsStayInTheirOrderAfterAChange() {
        InMemoryItemCatalog catalog = new InMemoryItemCatalog();
        List<ItemId> before = ids(catalog.all());

        new ChangeStatus(catalog).change(new ItemId("white-shirt"), ItemStatus.IN_WASH);

        assertEquals(before, ids(catalog.all()));
        assertEquals(ItemStatus.IN_WASH, catalog.find(new ItemId("white-shirt")).orElseThrow().status());
    }

    @Test
    void allIsASnapshot() {
        InMemoryItemCatalog catalog = new InMemoryItemCatalog();
        List<Item> snapshot = catalog.all();

        new ChangeStatus(catalog).change(new ItemId("white-shirt"), ItemStatus.IN_WASH);

        assertEquals(ItemStatus.CLEAN, snapshot.getFirst().status());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(item("x", ItemStatus.CLEAN)));
    }

    @Test
    void anIllegalChangeLeavesTheCatalogueUnchanged() {
        InMemoryItemCatalog catalog = new InMemoryItemCatalog();
        List<Item> before = catalog.all();

        // blue-jeans are in the wash; a trip straight from the wash is not in the table
        assertThrows(IllegalStatusChange.class,
                () -> new ChangeStatus(catalog).change(new ItemId("blue-jeans"), ItemStatus.ON_TRIP));
        assertEquals(before, catalog.all());
    }

    @Test
    void unknownIdsAreRejected() {
        InMemoryItemCatalog catalog = new InMemoryItemCatalog();
        List<Item> before = catalog.all();

        assertTrue(catalog.find(new ItemId("nope")).isEmpty());
        assertThrows(UnknownItem.class, () -> catalog.replace(item("nope", ItemStatus.IN_WASH)));
        assertThrows(UnknownItem.class,
                () -> new ChangeStatus(catalog).change(new ItemId("nope"), ItemStatus.IN_WASH));
        assertEquals(before, catalog.all());
    }

    @Test
    void duplicateSeedIdsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new InMemoryItemCatalog(List.of(item("a", ItemStatus.CLEAN), item("a", ItemStatus.CLEAN))));
    }

    @Test
    void concurrentChangesOfDifferentItemsLoseNone() throws Exception {
        int threads = 8;
        int changes = 100;
        List<Item> seed = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            seed.add(item("item-" + t, ItemStatus.CLEAN));
        }
        InMemoryItemCatalog catalog = new InMemoryItemCatalog(seed);
        ChangeStatus useCase = new ChangeStatus(catalog);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> done = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                ItemId id = new ItemId("item-" + t);
                done.add(pool.submit(() -> {
                    start.await();
                    for (int n = 0; n < changes; n++) {
                        // each change must see the previous one, or withStatus would throw
                        useCase.change(id, n % 2 == 0 ? ItemStatus.IN_WASH : ItemStatus.CLEAN);
                        catalog.all(); // readers run alongside the writers
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : done) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        // 100 alternating changes from CLEAN end in CLEAN; the order and the count are kept
        assertEquals(ids(seed), ids(catalog.all()));
        catalog.all().forEach(i -> assertEquals(ItemStatus.CLEAN, i.status(), i.id().value()));
    }
}
