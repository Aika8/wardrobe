package wardrobe.domain.avatar;

/**
 * A measurement of a figure is out of its range or contradicts another one. {@link #field()} names the measurement
 * as the {@link Measurements} component is named (for example {@code "waist"}).
 */
public final class InvalidMeasurement extends IllegalArgumentException {
    private final String field;

    public InvalidMeasurement(String field, String message) {
        super(field + ": " + message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
