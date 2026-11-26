package wardrobe.domain.avatar;

/**
 * Draft — вид проверяет Aika. A flat front view of a figure built from its {@link Measurements}: where the lines
 * of the body are. Origin at the top of the head, y grows down, all integer millimetres, integer arithmetic only.
 *
 * <p>Landmarks (y): head {@code 0}, neck, shoulders, chest, waist, hips, crotch ({@code height − inseam}), knee,
 * ankle, floor ({@code height}). Neck and shoulders are fixed shares of the height; chest, waist and hips are fixed
 * shares of the torso between the shoulder line and the crotch; knee and ankle are fixed shares of the inseam. That
 * keeps the landmarks ordered top to bottom for every valid {@link Measurements}.
 *
 * <p>Half-widths (x from the middle line): shoulders are a width, so half of it; chest, waist and hips are girths,
 * turned into a front width as {@code girth * 100 / 314} (the girth read as a circle) and halved. Every division
 * rounds down. The approximation and its error are in {@code DECISIONS.md} (WR-017).
 */
public record Figure(
        int head,
        int neck,
        int shoulders,
        int chest,
        int waist,
        int hips,
        int crotch,
        int knee,
        int ankle,
        int floor,
        int shoulderHalfWidth,
        int chestHalfWidth,
        int waistHalfWidth,
        int hipHalfWidth) {

    /** Neck line: this many thousandths of the height below the top of the head. */
    static final int NECK_PER_MILLE = 130;
    /** Shoulder line: this many thousandths of the height below the top of the head. */
    static final int SHOULDERS_PER_MILLE = 180;
    /** Chest, waist, hips: this many thousandths of the torso (shoulder line to crotch) below the shoulder line. */
    static final int CHEST_PER_MILLE_OF_TORSO = 250;
    static final int WAIST_PER_MILLE_OF_TORSO = 550;
    static final int HIPS_PER_MILLE_OF_TORSO = 850;
    /** Knee and ankle: this many thousandths of the inseam below the crotch. */
    static final int KNEE_PER_MILLE_OF_INSEAM = 380;
    static final int ANKLE_PER_MILLE_OF_INSEAM = 920;

    /** The figure of these measurements; the same measurements always give the same figure. */
    public static Figure of(Measurements m) {
        int height = m.height();
        int neck = height * NECK_PER_MILLE / 1000;
        int shoulders = height * SHOULDERS_PER_MILLE / 1000;
        int crotch = height - m.inseam();
        int torso = crotch - shoulders;
        return new Figure(
                0,
                neck,
                shoulders,
                shoulders + torso * CHEST_PER_MILLE_OF_TORSO / 1000,
                shoulders + torso * WAIST_PER_MILLE_OF_TORSO / 1000,
                shoulders + torso * HIPS_PER_MILLE_OF_TORSO / 1000,
                crotch,
                crotch + m.inseam() * KNEE_PER_MILLE_OF_INSEAM / 1000,
                crotch + m.inseam() * ANKLE_PER_MILLE_OF_INSEAM / 1000,
                height,
                m.shoulders() / 2,
                halfWidthOfGirth(m.chest()),
                halfWidthOfGirth(m.waist()),
                halfWidthOfGirth(m.hips()));
    }

    /** A girth read as a circle: front width {@code girth * 100 / 314}, then halved; both round down. */
    static int halfWidthOfGirth(int girth) {
        return girth * 100 / 314 / 2;
    }

    /** The landmarks top to bottom: head, neck, shoulders, chest, waist, hips, crotch, knee, ankle, floor. */
    public int[] landmarks() {
        return new int[] {head, neck, shoulders, chest, waist, hips, crotch, knee, ankle, floor};
    }
}
