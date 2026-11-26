package wardrobe.domain.card;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/**
 * The one table of Russian words a model answer may give instead of the English enum words of {@code docs/cards.md}
 * (WR-013; its scope is written in {@code DECISIONS.md}). Exact words only, after trimming, lower-casing and reading
 * «ё» as «е»: a word not in the table stays unknown — never the closest match (intent 01, invariant 5).
 *
 * <p>Colours are listed in the four forms an adjective takes (masculine, feminine, neuter, plural), since a model
 * may agree the colour with the item («синяя рубашка»). A word belongs to one enum only: {@link Map#ofEntries}
 * refuses a duplicate key, so the table cannot quietly read one word two ways.
 */
final class RussianWords {

    private static final Map<String, Enum<?>> WORDS = Map.ofEntries(
            // ItemType
            Map.entry("верх", ItemType.TOP),
            Map.entry("низ", ItemType.BOTTOM),
            Map.entry("платье", ItemType.DRESS),
            Map.entry("верхняя одежда", ItemType.OUTERWEAR),
            Map.entry("обувь", ItemType.SHOES),
            Map.entry("аксессуар", ItemType.ACCESSORY),
            // Colour
            Map.entry("черный", Colour.BLACK), Map.entry("черная", Colour.BLACK),
            Map.entry("черное", Colour.BLACK), Map.entry("черные", Colour.BLACK),
            Map.entry("белый", Colour.WHITE), Map.entry("белая", Colour.WHITE),
            Map.entry("белое", Colour.WHITE), Map.entry("белые", Colour.WHITE),
            Map.entry("серый", Colour.GREY), Map.entry("серая", Colour.GREY),
            Map.entry("серое", Colour.GREY), Map.entry("серые", Colour.GREY),
            Map.entry("темно-синий", Colour.NAVY), Map.entry("темно-синяя", Colour.NAVY),
            Map.entry("темно-синее", Colour.NAVY), Map.entry("темно-синие", Colour.NAVY),
            Map.entry("бежевый", Colour.BEIGE), Map.entry("бежевая", Colour.BEIGE),
            Map.entry("бежевое", Colour.BEIGE), Map.entry("бежевые", Colour.BEIGE),
            Map.entry("коричневый", Colour.BROWN), Map.entry("коричневая", Colour.BROWN),
            Map.entry("коричневое", Colour.BROWN), Map.entry("коричневые", Colour.BROWN),
            Map.entry("красный", Colour.RED), Map.entry("красная", Colour.RED),
            Map.entry("красное", Colour.RED), Map.entry("красные", Colour.RED),
            Map.entry("розовый", Colour.PINK), Map.entry("розовая", Colour.PINK),
            Map.entry("розовое", Colour.PINK), Map.entry("розовые", Colour.PINK),
            Map.entry("оранжевый", Colour.ORANGE), Map.entry("оранжевая", Colour.ORANGE),
            Map.entry("оранжевое", Colour.ORANGE), Map.entry("оранжевые", Colour.ORANGE),
            Map.entry("желтый", Colour.YELLOW), Map.entry("желтая", Colour.YELLOW),
            Map.entry("желтое", Colour.YELLOW), Map.entry("желтые", Colour.YELLOW),
            Map.entry("зеленый", Colour.GREEN), Map.entry("зеленая", Colour.GREEN),
            Map.entry("зеленое", Colour.GREEN), Map.entry("зеленые", Colour.GREEN),
            Map.entry("синий", Colour.BLUE), Map.entry("синяя", Colour.BLUE),
            Map.entry("синее", Colour.BLUE), Map.entry("синие", Colour.BLUE),
            Map.entry("фиолетовый", Colour.PURPLE), Map.entry("фиолетовая", Colour.PURPLE),
            Map.entry("фиолетовое", Colour.PURPLE), Map.entry("фиолетовые", Colour.PURPLE),
            // Pattern
            Map.entry("однотонный", Pattern.PLAIN),
            Map.entry("в полоску", Pattern.STRIPED),
            Map.entry("в клетку", Pattern.CHECKED),
            Map.entry("цветочный", Pattern.FLORAL),
            Map.entry("в горошек", Pattern.DOTTED),
            Map.entry("с принтом", Pattern.PRINTED),
            // Fabric
            Map.entry("хлопок", Fabric.COTTON),
            Map.entry("лен", Fabric.LINEN),
            Map.entry("шерсть", Fabric.WOOL),
            Map.entry("шелк", Fabric.SILK),
            Map.entry("деним", Fabric.DENIM),
            Map.entry("кожа", Fabric.LEATHER),
            Map.entry("трикотаж", Fabric.KNIT),
            Map.entry("синтетика", Fabric.SYNTHETIC),
            // Season
            Map.entry("зима", Season.WINTER),
            Map.entry("весна", Season.SPRING),
            Map.entry("лето", Season.SUMMER),
            Map.entry("осень", Season.AUTUMN),
            // Formality
            Map.entry("спортивный", Formality.SPORT),
            Map.entry("повседневный", Formality.CASUAL),
            Map.entry("смарт-кэжуал", Formality.SMART_CASUAL),
            Map.entry("деловой", Formality.BUSINESS),
            Map.entry("официальный", Formality.FORMAL));

    private RussianWords() {
    }

    /** The constant of {@code type} the Russian word names, if the table has exactly this word for that enum. */
    static <E extends Enum<E>> Optional<E> lookup(Class<E> type, String word) {
        String key = word.strip().toLowerCase(Locale.ROOT).replace('ё', 'е');
        Enum<?> found = WORDS.get(key);
        return type.isInstance(found) ? Optional.of(type.cast(found)) : Optional.empty();
    }
}
