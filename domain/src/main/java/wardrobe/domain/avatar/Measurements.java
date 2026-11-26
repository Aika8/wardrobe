package wardrobe.domain.avatar;

/**
 * Measurements of a figure, all integer millimetres (intent 01, invariant 6): height; shoulders as a width;
 * chest, waist and hips as girths; arm from shoulder to wrist; inseam. Ranges are for an adult and generous on
 * purpose; a value outside its range, or an arm or inseam not shorter than the height, is rejected with an
 * {@link InvalidMeasurement} that names the field.
 */
public record Measurements(int height, int shoulders, int chest, int waist, int hips, int arm, int inseam) {

    public static final int MIN_HEIGHT = 1300;
    public static final int MAX_HEIGHT = 2200;
    public static final int MIN_SHOULDERS = 300;
    public static final int MAX_SHOULDERS = 600;
    public static final int MIN_CHEST = 600;
    public static final int MAX_CHEST = 1600;
    public static final int MIN_WAIST = 450;
    public static final int MAX_WAIST = 1500;
    public static final int MIN_HIPS = 650;
    public static final int MAX_HIPS = 1700;
    public static final int MIN_ARM = 450;
    public static final int MAX_ARM = 800;
    public static final int MIN_INSEAM = 600;
    public static final int MAX_INSEAM = 1000;

    public Measurements {
        inRange("height", height, MIN_HEIGHT, MAX_HEIGHT);
        inRange("shoulders", shoulders, MIN_SHOULDERS, MAX_SHOULDERS);
        inRange("chest", chest, MIN_CHEST, MAX_CHEST);
        inRange("waist", waist, MIN_WAIST, MAX_WAIST);
        inRange("hips", hips, MIN_HIPS, MAX_HIPS);
        inRange("arm", arm, MIN_ARM, MAX_ARM);
        inRange("inseam", inseam, MIN_INSEAM, MAX_INSEAM);
        shorterThanHeight("inseam", inseam, height);
        shorterThanHeight("arm", arm, height);
    }

    private static void inRange(String field, int value, int min, int max) {
        if (value < min || value > max) {
            throw new InvalidMeasurement(field,
                    value + " mm is outside " + min + "–" + max + " mm");
        }
    }

    /**
     * Arm and inseam must be shorter than the height. With today's ranges (arm at most 800, inseam at most 1000,
     * height at least 1300) this always holds; it stays as its own guard so a widened range cannot break it.
     */
    static void shorterThanHeight(String field, int value, int height) {
        if (value >= height) {
            throw new InvalidMeasurement(field,
                    value + " mm must be less than the height " + height + " mm");
        }
    }
}
