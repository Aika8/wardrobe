package wardrobe.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import wardrobe.domain.Colour;
import wardrobe.domain.Pattern;

/** WR-012: a clean model answer (docs/cards.md) parses into the card its {@code .expected} file states. */
class CardParserCleanTest {

    @Test
    void cleanLowerCaseAnswer() throws IOException {
        assertMatchesExpected("01-clean-wool-coat");
    }

    @Test
    void cleanCapitalisedAnswer() throws IOException {
        assertMatchesExpected("02-clean-striped-top");
    }

    @Test
    void cleanAnswerWithExtraSpacesAndSeasonsOutOfOrder() throws IOException {
        assertMatchesExpected("03-clean-linen-trousers");
    }

    @Test
    void aWordOutsideTheListStaysUnknown() {
        CardDraft draft = CardParser.parse("colour: blueish\ntype: jumpsuit\nseasons: winter, monsoon\n");

        assertEquals(Colour.UNKNOWN, draft.colour());
        assertEquals(Optional.empty(), draft.type());
        assertEquals(0, draft.seasons().size());
    }

    @Test
    void aKeyGivenTwiceDifferentlyStaysUnknownAndAlikeTwiceIsKept() {
        CardDraft draft = CardParser.parse("colour: red\ncolour: pink\npattern: plain\npattern: PLAIN\n");

        assertEquals(Colour.UNKNOWN, draft.colour());
        assertEquals(Pattern.PLAIN, draft.pattern());
    }

    @Test
    void anEmptyAnswerIsAnUnknownCard() {
        assertEquals(CardDraft.unknown(), CardParser.parse(""));
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
        try (InputStream in = CardParserCleanTest.class.getResourceAsStream("/cards/" + name)) {
            assertNotNull(in, "missing fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
