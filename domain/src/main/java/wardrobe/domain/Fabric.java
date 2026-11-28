package wardrobe.domain;

/**
 * The main fabric of an item.
 *
 * <p>Each fabric states its weight as a constructor argument, so a new fabric cannot be added without deciding it
 * (same shape as {@link Colour#neutral()}). The table is Aika's to tune (DECISIONS.md, WR-037). {@code UNKNOWN} has
 * an unknown weight: never guessed (intent 01, invariant 5).
 */
public enum Fabric {
    COTTON(FabricWeight.LIGHT),
    LINEN(FabricWeight.LIGHT),
    WOOL(FabricWeight.HEAVY),
    SILK(FabricWeight.LIGHT),
    DENIM(FabricWeight.MEDIUM),
    LEATHER(FabricWeight.HEAVY),
    KNIT(FabricWeight.MEDIUM),
    SYNTHETIC(FabricWeight.MEDIUM),
    UNKNOWN(FabricWeight.UNKNOWN);

    private final FabricWeight weight;

    Fabric(FabricWeight weight) {
        this.weight = weight;
    }

    /** How warm this fabric is; {@link FabricWeight#UNKNOWN} for {@code UNKNOWN}. */
    public FabricWeight weight() {
        return weight;
    }
}
