package wardrobe.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChangeStatusTest {

    static Item item(String id, ItemStatus status) {
        return new Item(new ItemId(id), id, ItemType.TOP, Colour.WHITE, Pattern.PLAIN, Fabric.COTTON,
                Set.of(Season.SUMMER), Formality.CASUAL, status);
    }

    @Test
    void aLegalChangeIsStoredAndReturned() {
        FakeCatalog catalog = new FakeCatalog(item("a", ItemStatus.CLEAN), item("b", ItemStatus.CLEAN));
        Item changed = new ChangeStatus(catalog).change(new ItemId("b"), ItemStatus.IN_WASH);

        assertEquals(ItemStatus.IN_WASH, changed.status());
        assertEquals(List.of(item("a", ItemStatus.CLEAN), item("b", ItemStatus.IN_WASH)), catalog.all());
    }

    @Test
    void anIllegalChangeLeavesTheCatalogueUnchanged() {
        FakeCatalog catalog = new FakeCatalog(item("a", ItemStatus.IN_WASH));
        List<Item> before = catalog.all();

        assertThrows(IllegalStatusChange.class,
                () -> new ChangeStatus(catalog).change(new ItemId("a"), ItemStatus.ON_TRIP));
        assertEquals(before, catalog.all());
        assertEquals(0, catalog.replaces);
    }

    @Test
    void stayingInTheSameStatusIsNotAChange() {
        FakeCatalog catalog = new FakeCatalog(item("a", ItemStatus.CLEAN));
        assertThrows(IllegalStatusChange.class,
                () -> new ChangeStatus(catalog).change(new ItemId("a"), ItemStatus.CLEAN));
        assertEquals(0, catalog.replaces);
    }

    @Test
    void anUnknownItemIsReportedWithItsId() {
        FakeCatalog catalog = new FakeCatalog(item("a", ItemStatus.CLEAN));
        UnknownItem e = assertThrows(UnknownItem.class,
                () -> new ChangeStatus(catalog).change(new ItemId("nope"), ItemStatus.IN_WASH));
        assertEquals(new ItemId("nope"), e.id());
        assertEquals(List.of(item("a", ItemStatus.CLEAN)), catalog.all());
    }
}
