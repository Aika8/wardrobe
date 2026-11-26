package wardrobe.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/** WR-015: a draft becomes a clean item only when no field is unknown after Aika's corrections. */
class ConfirmCardTest {

    private static final ItemId ID = new ItemId("coat-1");

    private static CardDraft complete() {
        return new CardDraft(Optional.of("Wool coat"), Optional.of(ItemType.OUTERWEAR), Colour.NAVY, Pattern.PLAIN,
                Fabric.WOOL, EnumSet.of(Season.WINTER, Season.AUTUMN), Optional.of(Formality.SMART_CASUAL));
    }

    @Test
    void aCompleteDraftBecomesACleanItem() {
        Item item = ConfirmCard.confirm(ID, complete(), Corrections.none());

        assertEquals(new Item(ID, "Wool coat", ItemType.OUTERWEAR, Colour.NAVY, Pattern.PLAIN, Fabric.WOOL,
                EnumSet.of(Season.WINTER, Season.AUTUMN), Formality.SMART_CASUAL, ItemStatus.CLEAN), item);
    }

    @Test
    void correctionsWinOverTheDraft() {
        Corrections corrections = Corrections.none()
                .withName("Long coat").withType(ItemType.DRESS).withColour(Colour.BLACK)
                .withPattern(Pattern.CHECKED).withFabric(Fabric.SYNTHETIC)
                .withSeasons(Set.of(Season.SPRING)).withFormality(Formality.FORMAL);

        Item item = ConfirmCard.confirm(ID, complete(), corrections);

        assertEquals(new Item(ID, "Long coat", ItemType.DRESS, Colour.BLACK, Pattern.CHECKED, Fabric.SYNTHETIC,
                Set.of(Season.SPRING), Formality.FORMAL, ItemStatus.CLEAN), item);
    }

    @Test
    void anUnknownDraftWithoutCorrectionsListsEveryKeyInCardOrder() {
        IncompleteCard e = assertThrows(IncompleteCard.class,
                () -> ConfirmCard.confirm(ID, CardDraft.unknown(), Corrections.none()));

        assertEquals(List.of("type", "colour", "pattern", "fabric", "seasons", "formality", "name"), e.keys());
    }

    @Test
    void anUnknownDraftBecomesAnItemWhenAikaSuppliesEveryField() {
        Corrections all = Corrections.none()
                .withName("Scarf").withType(ItemType.ACCESSORY).withColour(Colour.RED)
                .withPattern(Pattern.DOTTED).withFabric(Fabric.SILK)
                .withSeasons(Set.of(Season.WINTER)).withFormality(Formality.CASUAL);

        Item item = ConfirmCard.confirm(ID, CardDraft.unknown(), all);

        assertEquals(ItemStatus.CLEAN, item.status());
        assertEquals(Colour.RED, item.colour());
        assertEquals("Scarf", item.name());
    }

    @Test
    void onlyTheFieldsStillUnknownAreListed() {
        CardDraft draft = new CardDraft(Optional.of("Scarf"), Optional.empty(), Colour.UNKNOWN, Pattern.PLAIN,
                Fabric.UNKNOWN, Set.of(), Optional.of(Formality.CASUAL));
        Corrections someFixed = Corrections.none().withColour(Colour.GREEN);

        IncompleteCard e = assertThrows(IncompleteCard.class, () -> ConfirmCard.confirm(ID, draft, someFixed));

        assertEquals(List.of("type", "fabric", "seasons"), e.keys());
    }

    @Test
    void unknownColourPatternOrFabricCannotBeConfirmedAsSuch() {
        CardDraft draft = new CardDraft(Optional.of("Top"), Optional.of(ItemType.TOP), Colour.UNKNOWN,
                Pattern.UNKNOWN, Fabric.UNKNOWN, Set.of(Season.SUMMER), Optional.of(Formality.CASUAL));
        Corrections unknownByHand = Corrections.none()
                .withColour(Colour.UNKNOWN).withPattern(Pattern.UNKNOWN).withFabric(Fabric.UNKNOWN);

        IncompleteCard e = assertThrows(IncompleteCard.class, () -> ConfirmCard.confirm(ID, draft, unknownByHand));

        assertEquals(List.of("colour", "pattern", "fabric"), e.keys());
    }

    @Test
    void aBlankCorrectedNameDoesNotHideTheDraftName() {
        Item item = ConfirmCard.confirm(ID, complete(), Corrections.none().withName("   "));

        assertEquals("Wool coat", item.name());
    }

    @Test
    void aBlankNameEverywhereIsUnknown() {
        CardDraft draft = new CardDraft(Optional.of("  "), Optional.of(ItemType.TOP), Colour.WHITE, Pattern.PLAIN,
                Fabric.COTTON, Set.of(Season.SUMMER), Optional.of(Formality.CASUAL));

        IncompleteCard e = assertThrows(IncompleteCard.class,
                () -> ConfirmCard.confirm(ID, draft, Corrections.none().withName("")));

        assertEquals(List.of("name"), e.keys());
    }

    @Test
    void aCorrectedNameIsTrimmed() {
        Item item = ConfirmCard.confirm(ID, complete(), Corrections.none().withName("  Navy coat "));

        assertEquals("Navy coat", item.name());
    }
}
