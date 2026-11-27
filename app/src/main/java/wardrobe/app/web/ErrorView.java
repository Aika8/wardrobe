package wardrobe.app.web;

/** JSON shape of an error, as in {@code api/openapi.yaml} (schema {@code Error}). */
public record ErrorView(String code, String message) {
    static final String UNKNOWN_ITEM = "UNKNOWN_ITEM";
    static final String ILLEGAL_STATUS_CHANGE = "ILLEGAL_STATUS_CHANGE";
    static final String IMPOSSIBLE_OUTFIT = "IMPOSSIBLE_OUTFIT";
    static final String BAD_REQUEST = "BAD_REQUEST";
}
