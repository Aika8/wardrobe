package wardrobe.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import wardrobe.domain.Colour;
import wardrobe.domain.card.Doubt.Why;

/** WR-014: {@code read} gives the same draft as {@code parse} plus one doubt per field it left unknown. */
class CardParserDoubtsTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "01-clean-wool-coat", "02-clean-striped-top", "03-clean-linen-trousers", "04-fenced-silk-dress",
        "05-russian-cotton-shirt", "06-missing-fabric-sneakers", "07-garbled-colour-scarf",
        "08-doubled-colour-skirt", "09-extra-keys-knit-sweater", "10-empty-answer"})
    void everyFixtureReadsToItsExpectedCardAndParseAgrees(String fixture) throws IOException {
        String answer = resource(fixture + ".txt");
        ParsedCard read = CardParser.read(answer);

        assertEquals(resource(fixture + ".expected"), render(read.draft()), fixture);
        assertEquals(CardParser.parse(answer), read.draft(), fixture);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "01-clean-wool-coat", "02-clean-striped-top", "03-clean-linen-trousers", "04-fenced-silk-dress",
        "05-russian-cotton-shirt", "09-extra-keys-knit-sweater"})
    void aFullyReadCardHasNoDoubts(String fixture) throws IOException {
        assertEquals(List.of(), CardParser.read(resource(fixture + ".txt")).doubts(), fixture);
    }

    @Test
    void missingFabricIsOneMissingDoubt() throws IOException {
        assertEquals(List.of(new Doubt("fabric", "", Why.MISSING)),
                CardParser.read(resource("06-missing-fabric-sneakers.txt")).doubts());
    }

    @Test
    void garbledColourIsUnreadableWithTheRawValue() throws IOException {
        assertEquals(List.of(new Doubt("colour", "blueish ?", Why.UNREADABLE)),
                CardParser.read(resource("07-garbled-colour-scarf.txt")).doubts());
    }

    @Test
    void doubledColourIsConflictingWithBothValuesNotTheFirstOne() throws IOException {
        ParsedCard read = CardParser.read(resource("08-doubled-colour-skirt.txt"));

        assertEquals(Colour.UNKNOWN, read.draft().colour());
        assertEquals(List.of(new Doubt("colour", "red | pink", Why.CONFLICTING)), read.doubts());
    }

    @Test
    void emptyAnswerHasOneMissingDoubtPerKeyInCardOrder() throws IOException {
        List<Doubt> expected = List.of("type", "colour", "pattern", "fabric", "seasons", "formality", "name")
                .stream().map(key -> new Doubt(key, "", Why.MISSING)).toList();

        assertEquals(expected, CardParser.read(resource("10-empty-answer.txt")).doubts());
        assertEquals(expected, CardParser.read(null).doubts());
    }

    @Test
    void theSameValueTwiceInAnotherCaseIsNoDoubt() {
        ParsedCard read = CardParser.read("colour: Red\ncolour: red\n");

        assertEquals(Colour.RED, read.draft().colour());
        assertEquals(Optional.empty(), read.doubts().stream().filter(d -> d.key().equals("colour")).findAny());
    }

    @Test
    void theSameColourInTwoLanguagesIsConflicting() {
        assertEquals(new Doubt("colour", "blue | синий", Why.CONFLICTING),
                doubt(CardParser.read("colour: blue\ncolour: синий\n"), "colour"));
    }

    @Test
    void aKeyWithNoValueIsMissing() {
        ParsedCard read = CardParser.read("name:\nseasons:   \n");

        assertEquals(new Doubt("name", "", Why.MISSING), doubt(read, "name"));
        assertEquals(new Doubt("seasons", "", Why.MISSING), doubt(read, "seasons"));
    }

    @Test
    void seasonsWithOneUnknownWordAreUnreadableAsAWhole() {
        ParsedCard read = CardParser.read("seasons: winter, monsoon\n");

        assertEquals(new Doubt("seasons", "winter, monsoon", Why.UNREADABLE), doubt(read, "seasons"));
        assertEquals(java.util.Set.of(), read.draft().seasons());
    }

    @Test
    void unreadableValuesInEveryEnumFieldKeepTheirRawText() {
        ParsedCard read = CardParser.read("""
                type: кофта
                colour: синеватый
                pattern: полоски
                fabric: хлопковый
                formality: нарядный
                """);

        assertEquals(new Doubt("type", "кофта", Why.UNREADABLE), doubt(read, "type"));
        assertEquals(new Doubt("colour", "синеватый", Why.UNREADABLE), doubt(read, "colour"));
        assertEquals(new Doubt("pattern", "полоски", Why.UNREADABLE), doubt(read, "pattern"));
        assertEquals(new Doubt("fabric", "хлопковый", Why.UNREADABLE), doubt(read, "fabric"));
        assertEquals(new Doubt("formality", "нарядный", Why.UNREADABLE), doubt(read, "formality"));
    }

    private static Doubt doubt(ParsedCard read, String key) {
        List<Doubt> forKey = read.doubts().stream().filter(d -> d.key().equals(key)).toList();
        assertEquals(1, forKey.size(), "doubts for " + key + ": " + read.doubts());
        return forKey.getFirst();
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
        try (InputStream in = CardParserDoubtsTest.class.getResourceAsStream("/cards/" + name)) {
            assertNotNull(in, "missing fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
