package wardrobe.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/** WR-013: prose and code fences around the card are skipped; Russian enum words are read through one table. */
class CardParserTolerantTest {

    @Test
    void cardInsideACodeFenceWithProseAround() throws IOException {
        assertMatchesExpected("04-fenced-silk-dress");
    }

    @Test
    void russianValuesAndARussianName() throws IOException {
        assertMatchesExpected("05-russian-cotton-shirt");
    }

    @Test
    void extraKeysAreSkipped() throws IOException {
        assertMatchesExpected("09-extra-keys-knit-sweater");
    }

    @Test
    void proseWithAColonAndAFenceWithALanguageAreSkipped() {
        CardDraft draft = CardParser.parse("""
                Sure! Note: I looked at the whole photo.
                ```yaml
                colour: red
                ```
                Hope this helps: tell me more.
                """);

        assertEquals(Colour.RED, draft.colour());
        assertEquals(Optional.empty(), draft.name());
    }

    @Test
    void russianWordsInEveryFieldIgnoringCaseSpacesAndYo() {
        CardDraft draft = CardParser.parse("""
                type: Верхняя одежда
                colour:  ЧЁРНОЕ
                pattern: в клетку
                fabric: Шёлк
                seasons: осень, Зима
                formality: деловой
                """);

        assertEquals(Optional.of(ItemType.OUTERWEAR), draft.type());
        assertEquals(Colour.BLACK, draft.colour());
        assertEquals(Pattern.CHECKED, draft.pattern());
        assertEquals(Fabric.SILK, draft.fabric());
        assertEquals(Set.of(Season.WINTER, Season.AUTUMN), draft.seasons());
        assertEquals(Optional.of(Formality.BUSINESS), draft.formality());
    }

    @Test
    void russianAndEnglishSeasonsMayBeMixed() {
        assertEquals(Set.of(Season.SPRING, Season.SUMMER), CardParser.parse("seasons: весна, summer").seasons());
    }

    @Test
    void aWordInNeitherLanguageStaysUnknownNotTheClosestMatch() {
        CardDraft draft = CardParser.parse("""
                type: кофта
                colour: синеватый
                pattern: полоски
                fabric: хлопковый
                seasons: зима, межсезонье
                formality: нарядный
                """);

        assertEquals(Optional.empty(), draft.type());
        assertEquals(Colour.UNKNOWN, draft.colour());
        assertEquals(Pattern.UNKNOWN, draft.pattern());
        assertEquals(Fabric.UNKNOWN, draft.fabric());
        assertEquals(Set.of(), draft.seasons());
        assertEquals(Optional.empty(), draft.formality());
    }

    @Test
    void aRussianWordOfAnotherFieldIsNotRead() {
        CardDraft draft = CardParser.parse("colour: хлопок\nfabric: синий\nseasons: лето, красный\n");

        assertEquals(Colour.UNKNOWN, draft.colour());
        assertEquals(Fabric.UNKNOWN, draft.fabric());
        assertEquals(Set.of(), draft.seasons());
    }

    @Test
    void theSameColourInTwoLanguagesIsStillTwoDifferentValues() {
        // WR-012 compares the raw values; WR-014 decides doubts. Here: different text → unknown, never a guess.
        assertEquals(Colour.UNKNOWN, CardParser.parse("colour: blue\ncolour: синий\n").colour());
    }

    @Test
    void cleanFixturesStillParse() throws IOException {
        assertMatchesExpected("01-clean-wool-coat");
        assertMatchesExpected("02-clean-striped-top");
        assertMatchesExpected("03-clean-linen-trousers");
        assertMatchesExpected("06-missing-fabric-sneakers");
        assertMatchesExpected("07-garbled-colour-scarf");
        assertMatchesExpected("08-doubled-colour-skirt");
        assertMatchesExpected("10-empty-answer");
    }

    private static void assertMatchesExpected(String fixture) throws IOException {
        CardDraft draft = CardParser.parse(resource(fixture + ".txt"));
        assertEquals(resource(fixture + ".expected"), render(draft), fixture);
    }

    /** The card in the {@code .expected} format of docs/cards.md. */
    private static String render(CardDraft draft) {
        String seasons = draft.seasons().isEmpty()
                ? "?"
                : draft.seasons().stream().map(Enum::name).collect(Collectors.joining(","));
        return "type: " + draft.type().map(Enum::name).orElse("?") + "\n"
                + "colour: " + draft.colour().name() + "\n"
                + "pattern: " + draft.pattern().name() + "\n"
                + "fabric: " + draft.fabric().name() + "\n"
                + "seasons: " + seasons + "\n"
                + "formality: " + draft.formality().map(Enum::name).orElse("?") + "\n"
                + "name: " + draft.name().orElse("?") + "\n";
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = CardParserTolerantTest.class.getResourceAsStream("/cards/" + name)) {
            assertNotNull(in, "missing fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
