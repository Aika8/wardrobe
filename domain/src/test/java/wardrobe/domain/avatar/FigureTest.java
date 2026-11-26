package wardrobe.domain.avatar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FigureTest {

    static Stream<Measurements> testFigures() {
        return Stream.of(TestFigures.SMALL, TestFigures.MEDIUM, TestFigures.LARGE);
    }

    /** The extremes of the valid ranges, where the torso is shortest or longest. */
    static Stream<Measurements> extremeFigures() {
        return Stream.of(
                new Measurements(1300, 300, 600, 450, 650, 450, 1000),
                new Measurements(1300, 600, 1600, 1500, 1700, 800, 600),
                new Measurements(2200, 300, 600, 450, 650, 450, 600),
                new Measurements(2200, 600, 1600, 1500, 1700, 800, 1000));
    }

    @ParameterizedTest
    @MethodSource({"testFigures", "extremeFigures"})
    void landmarksAreOrderedTopToBottom(Measurements m) {
        int[] y = Figure.of(m).landmarks();
        assertEquals(10, y.length);
        for (int i = 1; i < y.length; i++) {
            assertTrue(y[i - 1] < y[i], "landmark " + i + " (" + y[i] + ") must be below " + (i - 1) + " (" + y[i - 1] + ")");
        }
    }

    @ParameterizedTest
    @MethodSource("testFigures")
    void headFloorAndCrotchComeStraightFromTheMeasurements(Measurements m) {
        Figure f = Figure.of(m);
        assertEquals(0, f.head());
        assertEquals(m.height(), f.floor());
        assertEquals(m.height() - m.inseam(), f.crotch());
    }

    @ParameterizedTest
    @MethodSource("testFigures")
    void halfWidthsFollowTheGirths(Measurements m) {
        Figure f = Figure.of(m);
        assertEquals(m.shoulders() / 2, f.shoulderHalfWidth());
        assertEquals(m.chest() * 100 / 314 / 2, f.chestHalfWidth());
        assertEquals(m.waist() * 100 / 314 / 2, f.waistHalfWidth());
        assertEquals(m.hips() * 100 / 314 / 2, f.hipHalfWidth());
    }

    @Test
    void middleFigureInNumbers() {
        // height 1680, shoulders 400, chest 920, waist 740, hips 990, inseam 770
        Figure f = Figure.of(TestFigures.MEDIUM);
        assertEquals(new Figure(0, 218, 302, 454, 636, 818, 910, 1202, 1618, 1680, 200, 146, 117, 157), f);
    }

    @Test
    void largerFiguresAreWiderEverywhere() {
        Figure small = Figure.of(TestFigures.SMALL);
        Figure medium = Figure.of(TestFigures.MEDIUM);
        Figure large = Figure.of(TestFigures.LARGE);
        assertTrue(small.waistHalfWidth() < medium.waistHalfWidth());
        assertTrue(medium.waistHalfWidth() < large.waistHalfWidth());
        assertTrue(small.hipHalfWidth() < medium.hipHalfWidth());
        assertTrue(medium.hipHalfWidth() < large.hipHalfWidth());
    }

    @Test
    void aWiderWaistGivesAWiderWaistHalfWidth() {
        Measurements m = TestFigures.MEDIUM;
        Measurements wider = new Measurements(m.height(), m.shoulders(), m.chest(), m.waist() + 100, m.hips(), m.arm(),
                m.inseam());
        Figure before = Figure.of(m);
        Figure after = Figure.of(wider);
        assertTrue(after.waistHalfWidth() > before.waistHalfWidth());
        assertEquals(before.hipHalfWidth(), after.hipHalfWidth());
        assertEquals(before.chestHalfWidth(), after.chestHalfWidth());
        assertEquals(before.waist(), after.waist(), "a girth moves no line of the body");
    }

    @ParameterizedTest
    @MethodSource("testFigures")
    void theSameInputGivesTheSameFigure(Measurements m) {
        Measurements copy = new Measurements(m.height(), m.shoulders(), m.chest(), m.waist(), m.hips(), m.arm(),
                m.inseam());
        assertEquals(Figure.of(m), Figure.of(copy));
        assertEquals(Figure.of(m), Figure.of(m));
    }
}
