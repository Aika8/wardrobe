package wardrobe.domain.avatar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class MeasurementsTest {

    private static final Measurements M = TestFigures.MEDIUM;

    /** One field changed, the others as in the middle figure. */
    record Field(String name, int min, int max, IntFunction<Measurements> with) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Field> fields() {
        return Stream.of(
                new Field("height", 1300, 2200,
                        v -> new Measurements(v, M.shoulders(), M.chest(), M.waist(), M.hips(), M.arm(), M.inseam())),
                new Field("shoulders", 300, 600,
                        v -> new Measurements(M.height(), v, M.chest(), M.waist(), M.hips(), M.arm(), M.inseam())),
                new Field("chest", 600, 1600,
                        v -> new Measurements(M.height(), M.shoulders(), v, M.waist(), M.hips(), M.arm(), M.inseam())),
                new Field("waist", 450, 1500,
                        v -> new Measurements(M.height(), M.shoulders(), M.chest(), v, M.hips(), M.arm(), M.inseam())),
                new Field("hips", 650, 1700,
                        v -> new Measurements(M.height(), M.shoulders(), M.chest(), M.waist(), v, M.arm(), M.inseam())),
                new Field("arm", 450, 800,
                        v -> new Measurements(M.height(), M.shoulders(), M.chest(), M.waist(), M.hips(), v, M.inseam())),
                new Field("inseam", 600, 1000,
                        v -> new Measurements(M.height(), M.shoulders(), M.chest(), M.waist(), M.hips(), M.arm(), v)));
    }

    @Test
    void theTestFiguresAreValid() {
        assertEquals(1550, TestFigures.SMALL.height());
        assertEquals(740, TestFigures.MEDIUM.waist());
        assertEquals(860, TestFigures.LARGE.inseam());
    }

    @ParameterizedTest
    @MethodSource("fields")
    void bothEndsOfTheRangeAreAccepted(Field field) {
        assertDoesNotThrow(() -> field.with().apply(field.min()));
        assertDoesNotThrow(() -> field.with().apply(field.max()));
    }

    @ParameterizedTest
    @MethodSource("fields")
    void oneMillimetreBelowTheRangeIsRejectedWithTheFieldsName(Field field) {
        InvalidMeasurement e = assertThrows(InvalidMeasurement.class, () -> field.with().apply(field.min() - 1));
        assertEquals(field.name(), e.field());
        assertTrue(e.getMessage().startsWith(field.name() + ":"), e.getMessage());
    }

    @ParameterizedTest
    @MethodSource("fields")
    void oneMillimetreAboveTheRangeIsRejectedWithTheFieldsName(Field field) {
        InvalidMeasurement e = assertThrows(InvalidMeasurement.class, () -> field.with().apply(field.max() + 1));
        assertEquals(field.name(), e.field());
    }

    @ParameterizedTest
    @MethodSource("fields")
    void zeroAndNegativeValuesAreRejected(Field field) {
        assertEquals(field.name(), assertThrows(InvalidMeasurement.class, () -> field.with().apply(0)).field());
        assertEquals(field.name(), assertThrows(InvalidMeasurement.class, () -> field.with().apply(-1)).field());
    }

    @ParameterizedTest
    @ValueSource(strings = {"arm", "inseam"})
    void aLengthNotShorterThanTheHeightIsRejectedWithTheFieldsName(String name) {
        assertEquals(name, assertThrows(InvalidMeasurement.class,
                () -> Measurements.shorterThanHeight(name, 1700, 1700)).field());
        assertEquals(name, assertThrows(InvalidMeasurement.class,
                () -> Measurements.shorterThanHeight(name, 1701, 1700)).field());
        assertDoesNotThrow(() -> Measurements.shorterThanHeight(name, 1699, 1700));
    }

    @Test
    void everyComponentIsAnIntInMillimetres() {
        for (RecordComponent c : Measurements.class.getRecordComponents()) {
            assertEquals(int.class, c.getType(), c.getName());
        }
        assertEquals(7, Measurements.class.getRecordComponents().length);
    }

    @Test
    void fieldNamesInTheErrorMatchTheRecordComponents() {
        var names = Stream.of(Measurements.class.getRecordComponents()).map(RecordComponent::getName).toList();
        assertEquals(fields().map(Field::name).toList(), names);
    }
}
