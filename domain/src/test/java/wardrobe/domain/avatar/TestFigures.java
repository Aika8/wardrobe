package wardrobe.domain.avatar;

/**
 * Invented figures for tests (intent 01, invariant 7): none of them is anyone's real measurements.
 */
public final class TestFigures {

    /** A short, slim figure. */
    public static final Measurements SMALL = new Measurements(1550, 360, 820, 640, 880, 520, 700);

    /** A middle figure. */
    public static final Measurements MEDIUM = new Measurements(1680, 400, 920, 740, 990, 570, 770);

    /** A tall, broad figure. */
    public static final Measurements LARGE = new Measurements(1860, 470, 1120, 960, 1140, 640, 860);

    private TestFigures() {
    }
}
