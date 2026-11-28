package wardrobe.app.catalog;

import static wardrobe.domain.Colour.BEIGE;
import static wardrobe.domain.Colour.BLACK;
import static wardrobe.domain.Colour.BLUE;
import static wardrobe.domain.Colour.BROWN;
import static wardrobe.domain.Colour.GREEN;
import static wardrobe.domain.Colour.GREY;
import static wardrobe.domain.Colour.NAVY;
import static wardrobe.domain.Colour.ORANGE;
import static wardrobe.domain.Colour.PINK;
import static wardrobe.domain.Colour.PURPLE;
import static wardrobe.domain.Colour.RED;
import static wardrobe.domain.Colour.WHITE;
import static wardrobe.domain.Colour.YELLOW;
import static wardrobe.domain.Fabric.COTTON;
import static wardrobe.domain.Fabric.DENIM;
import static wardrobe.domain.Fabric.KNIT;
import static wardrobe.domain.Fabric.LEATHER;
import static wardrobe.domain.Fabric.LINEN;
import static wardrobe.domain.Fabric.SILK;
import static wardrobe.domain.Fabric.SYNTHETIC;
import static wardrobe.domain.Fabric.WOOL;
import static wardrobe.domain.Formality.BUSINESS;
import static wardrobe.domain.Formality.CASUAL;
import static wardrobe.domain.Formality.FORMAL;
import static wardrobe.domain.Formality.SMART_CASUAL;
import static wardrobe.domain.Formality.SPORT;
import static wardrobe.domain.ItemStatus.CLEAN;
import static wardrobe.domain.ItemStatus.IN_REPAIR;
import static wardrobe.domain.ItemStatus.IN_WASH;
import static wardrobe.domain.ItemStatus.ON_TRIP;
import static wardrobe.domain.ItemType.ACCESSORY;
import static wardrobe.domain.ItemType.BOTTOM;
import static wardrobe.domain.ItemType.DRESS;
import static wardrobe.domain.ItemType.OUTERWEAR;
import static wardrobe.domain.ItemType.SHOES;
import static wardrobe.domain.ItemType.TOP;
import static wardrobe.domain.Pattern.CHECKED;
import static wardrobe.domain.Pattern.DOTTED;
import static wardrobe.domain.Pattern.FLORAL;
import static wardrobe.domain.Pattern.PLAIN;
import static wardrobe.domain.Pattern.PRINTED;
import static wardrobe.domain.Pattern.STRIPED;
import static wardrobe.domain.Season.AUTUMN;
import static wardrobe.domain.Season.SPRING;
import static wardrobe.domain.Season.SUMMER;
import static wardrobe.domain.Season.WINTER;

import java.util.List;
import java.util.Set;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/**
 * A bigger invented wardrobe of 60 items, so that suggestions, occasions and weather have something to work with.
 * Plain data: not a Spring bean and not wired anywhere (WR-048 puts it behind a profile). No photos, no real brands.
 *
 * <p>Built from four capsules, named by the id prefix: {@code office-}, {@code weekend-}, {@code sport-} and
 * {@code evening-}. Neutral colours dominate each capsule; its few accents are friends by the colour rule. The list
 * is grouped by {@link ItemType} order and, within a type, by capsule. Three items that are not clean carry one
 * unknown field each (colour, pattern, fabric), so the "never vouched for" path has data without blocking anything.
 */
final class SampleWardrobe {

    private static final Set<Season> ALL_YEAR = Set.of(WINTER, SPRING, SUMMER, AUTUMN);
    private static final Set<Season> WARM = Set.of(SPRING, SUMMER, AUTUMN);
    private static final Set<Season> COOL = Set.of(AUTUMN, WINTER, SPRING);
    private static final Set<Season> COLD = Set.of(AUTUMN, WINTER);
    private static final Set<Season> SPRING_SUMMER = Set.of(SPRING, SUMMER);

    private static final List<Item> ITEMS = List.of(
            // TOP
            item("office-white-cotton-shirt", "White cotton shirt", TOP, WHITE, PLAIN, COTTON, WARM, BUSINESS, CLEAN),
            item("office-blue-cotton-shirt", "Sky blue cotton shirt", TOP, BLUE, PLAIN, COTTON, ALL_YEAR, BUSINESS,
                    CLEAN),
            item("office-beige-silk-blouse", "Sand silk blouse", TOP, BEIGE, PLAIN, SILK, SPRING_SUMMER, SMART_CASUAL,
                    CLEAN),
            item("office-grey-wool-jumper", "Grey fine wool jumper", TOP, GREY, PLAIN, WOOL, COLD, SMART_CASUAL,
                    CLEAN),
            item("office-yellow-silk-blouse", "Butter yellow silk blouse", TOP, YELLOW, PLAIN, SILK, SPRING_SUMMER,
                    SMART_CASUAL, CLEAN),
            item("weekend-white-cotton-tee", "White cotton T-shirt", TOP, WHITE, PLAIN, COTTON, WARM, CASUAL, CLEAN),
            item("weekend-navy-striped-tee", "Navy striped T-shirt", TOP, NAVY, STRIPED, COTTON, WARM, CASUAL, CLEAN),
            item("weekend-pink-floral-blouse", "Pink floral blouse", TOP, PINK, FLORAL, COTTON, SPRING_SUMMER,
                    SMART_CASUAL, CLEAN),
            item("weekend-grey-knit-sweater", "Grey chunky knit sweater", TOP, GREY, PLAIN, KNIT, COLD, CASUAL, CLEAN),
            item("weekend-green-linen-shirt", "Sage linen shirt", TOP, GREEN, PLAIN, LINEN, Set.of(SUMMER), CASUAL,
                    CLEAN),
            item("weekend-beige-cardigan", "Oatmeal cardigan", TOP, BEIGE, PLAIN, Fabric.UNKNOWN, COOL, CASUAL,
                    IN_WASH),
            item("sport-black-training-tee", "Black training T-shirt", TOP, BLACK, PLAIN, SYNTHETIC, WARM, SPORT,
                    CLEAN),
            item("sport-grey-cotton-hoodie", "Grey cotton hoodie", TOP, GREY, PLAIN, COTTON, COOL, CASUAL, CLEAN),
            item("sport-orange-running-top", "Orange running top", TOP, ORANGE, PLAIN, SYNTHETIC, Set.of(SUMMER),
                    SPORT, IN_WASH),
            item("sport-thermal-base-layer", "Thermal base layer", TOP, Colour.UNKNOWN, PLAIN, SYNTHETIC,
                    Set.of(WINTER), SPORT, IN_REPAIR),
            item("evening-black-silk-camisole", "Black silk camisole", TOP, BLACK, PLAIN, SILK, SPRING_SUMMER, FORMAL,
                    CLEAN),
            item("evening-purple-silk-blouse", "Plum silk blouse", TOP, PURPLE, PLAIN, SILK, ALL_YEAR, BUSINESS,
                    ON_TRIP),
            // BOTTOM
            item("office-navy-wool-trousers", "Navy wool trousers", BOTTOM, NAVY, PLAIN, WOOL, COOL, BUSINESS, CLEAN),
            item("office-grey-wool-trousers", "Grey wool trousers", BOTTOM, GREY, PLAIN, WOOL, COLD, BUSINESS, CLEAN),
            item("office-black-pencil-skirt", "Black wool pencil skirt", BOTTOM, BLACK, PLAIN, WOOL, ALL_YEAR,
                    BUSINESS, CLEAN),
            item("office-beige-linen-trousers", "Sand linen trousers", BOTTOM, BEIGE, PLAIN, LINEN, Set.of(SUMMER),
                    SMART_CASUAL, CLEAN),
            item("weekend-blue-jeans", "Mid blue jeans", BOTTOM, BLUE, PLAIN, DENIM, ALL_YEAR, CASUAL, CLEAN),
            item("weekend-black-jeans", "Black jeans", BOTTOM, BLACK, PLAIN, DENIM, ALL_YEAR, CASUAL, CLEAN),
            item("weekend-beige-chinos", "Stone cotton chinos", BOTTOM, BEIGE, PLAIN, COTTON, WARM, SMART_CASUAL,
                    CLEAN),
            item("weekend-brown-checked-skirt", "Brown checked wool skirt", BOTTOM, BROWN, CHECKED, WOOL, COLD,
                    SMART_CASUAL, CLEAN),
            item("sport-black-leggings", "Black sports leggings", BOTTOM, BLACK, PLAIN, SYNTHETIC, ALL_YEAR, SPORT,
                    CLEAN),
            item("sport-navy-joggers", "Navy cotton joggers", BOTTOM, NAVY, PLAIN, COTTON, COOL, CASUAL, CLEAN),
            item("sport-blue-running-shorts", "Blue running shorts", BOTTOM, BLUE, PLAIN, SYNTHETIC, Set.of(SUMMER),
                    SPORT, CLEAN),
            item("evening-black-wool-trousers", "Black wide wool trousers", BOTTOM, BLACK, PLAIN, WOOL, COOL, FORMAL,
                    CLEAN),
            // DRESS
            item("office-black-wool-dress", "Black wool shift dress", DRESS, BLACK, PLAIN, WOOL, COLD, BUSINESS,
                    CLEAN),
            item("office-navy-dotted-dress", "Navy dotted cotton dress", DRESS, NAVY, DOTTED, COTTON, SPRING_SUMMER,
                    SMART_CASUAL, CLEAN),
            item("weekend-white-linen-dress", "White linen summer dress", DRESS, WHITE, PLAIN, LINEN, Set.of(SUMMER),
                    CASUAL, ON_TRIP),
            item("weekend-green-floral-dress", "Green floral tea dress", DRESS, GREEN, FLORAL, COTTON, SPRING_SUMMER,
                    SMART_CASUAL, IN_REPAIR),
            item("evening-red-silk-dress", "Red silk slip dress", DRESS, RED, PLAIN, SILK, Set.of(SUMMER), FORMAL,
                    CLEAN),
            item("evening-black-wool-dress", "Black wool evening dress", DRESS, BLACK, PLAIN, WOOL, COLD, FORMAL,
                    CLEAN),
            item("evening-navy-long-dress", "Navy long dress", DRESS, NAVY, Pattern.UNKNOWN, SILK, SPRING_SUMMER,
                    BUSINESS, ON_TRIP),
            // OUTERWEAR
            item("office-camel-wool-coat", "Camel wool coat", OUTERWEAR, BEIGE, PLAIN, WOOL, COLD, BUSINESS, CLEAN),
            item("office-beige-trench", "Beige cotton trench", OUTERWEAR, BEIGE, PLAIN, COTTON, Set.of(SPRING, AUTUMN),
                    SMART_CASUAL, CLEAN),
            item("office-grey-checked-blazer", "Grey checked wool blazer", OUTERWEAR, GREY, CHECKED, WOOL, COOL,
                    BUSINESS, IN_WASH),
            item("weekend-black-puffer", "Black puffer jacket", OUTERWEAR, BLACK, PLAIN, SYNTHETIC, Set.of(WINTER),
                    CASUAL, CLEAN),
            item("weekend-denim-jacket", "Blue denim jacket", OUTERWEAR, BLUE, PLAIN, DENIM, WARM, CASUAL, CLEAN),
            item("sport-navy-running-jacket", "Navy running jacket", OUTERWEAR, NAVY, PLAIN, SYNTHETIC, COLD, SPORT,
                    CLEAN),
            item("evening-black-wool-coat", "Black long wool coat", OUTERWEAR, BLACK, PLAIN, WOOL, COLD, FORMAL,
                    CLEAN),
            // SHOES
            item("office-black-loafers", "Black leather loafers", SHOES, BLACK, PLAIN, LEATHER, WARM, BUSINESS, CLEAN),
            item("office-brown-ankle-boots", "Brown leather ankle boots", SHOES, BROWN, PLAIN, LEATHER, COLD, BUSINESS,
                    CLEAN),
            item("weekend-white-sneakers", "White canvas sneakers", SHOES, WHITE, PLAIN, COTTON, WARM, CASUAL, CLEAN),
            item("weekend-brown-winter-boots", "Brown lace-up winter boots", SHOES, BROWN, PLAIN, LEATHER, COLD,
                    CASUAL, CLEAN),
            item("weekend-beige-sandals", "Tan leather sandals", SHOES, BEIGE, PLAIN, LEATHER, Set.of(SUMMER), CASUAL,
                    ON_TRIP),
            item("sport-white-running-shoes", "White running shoes", SHOES, WHITE, PLAIN, SYNTHETIC, WARM, SPORT,
                    CLEAN),
            item("sport-black-winter-trainers", "Black insulated trainers", SHOES, BLACK, PLAIN, SYNTHETIC, COLD,
                    SPORT, CLEAN),
            item("evening-black-heels", "Black leather heels", SHOES, BLACK, PLAIN, LEATHER, ALL_YEAR, FORMAL, CLEAN),
            item("evening-grey-suede-heels", "Grey suede heels", SHOES, GREY, PLAIN, LEATHER, COLD, BUSINESS,
                    IN_REPAIR),
            // ACCESSORY
            item("office-brown-leather-belt", "Brown leather belt", ACCESSORY, BROWN, PLAIN, LEATHER, ALL_YEAR,
                    BUSINESS, CLEAN),
            item("office-black-leather-bag", "Black leather work bag", ACCESSORY, BLACK, PLAIN, LEATHER, ALL_YEAR,
                    BUSINESS, CLEAN),
            item("weekend-grey-checked-scarf", "Grey checked wool scarf", ACCESSORY, GREY, CHECKED, WOOL, COLD,
                    CASUAL, CLEAN),
            item("weekend-linen-sun-hat", "Natural linen sun hat", ACCESSORY, BEIGE, PLAIN, LINEN, Set.of(SUMMER),
                    CASUAL, CLEAN),
            item("weekend-orange-tote", "Orange printed cotton tote", ACCESSORY, ORANGE, PRINTED, COTTON, WARM, CASUAL,
                    CLEAN),
            item("sport-grey-knit-beanie", "Grey knit beanie", ACCESSORY, GREY, PLAIN, KNIT, Set.of(WINTER), SPORT,
                    CLEAN),
            item("evening-pink-silk-clutch", "Blush silk clutch", ACCESSORY, PINK, PLAIN, SILK, ALL_YEAR, FORMAL,
                    CLEAN),
            item("evening-yellow-silk-scarf", "Gold printed silk scarf", ACCESSORY, YELLOW, PRINTED, SILK, WARM,
                    FORMAL, CLEAN));

    private SampleWardrobe() {
    }

    /** The 60 invented items: immutable, in a stable order grouped by type. */
    public static List<Item> items() {
        return ITEMS;
    }

    private static Item item(String id, String name, ItemType type, Colour colour, Pattern pattern, Fabric fabric,
                             Set<Season> seasons, Formality formality, ItemStatus status) {
        return new Item(new ItemId(id), name, type, colour, pattern, fabric, seasons, formality, status);
    }
}
