package wardrobe.app.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.domain.Item;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemType;
import wardrobe.domain.UnknownItem;
import wardrobe.domain.avatar.Figure;
import wardrobe.domain.avatar.Fit;
import wardrobe.domain.avatar.FitNote;
import wardrobe.domain.avatar.GarmentSize;
import wardrobe.domain.avatar.InvalidMeasurement;
import wardrobe.domain.avatar.Layers;
import wardrobe.domain.avatar.Measurements;
import wardrobe.domain.avatar.Placement;
import wardrobe.domain.outfit.Outfit;

/**
 * REST adapter: dress the draft avatar (contract: {@code api/openapi.yaml}, {@code POST /avatar/dress}). Reads the
 * request into domain values and calls the domain: {@link Figure#of}, {@link Layers#order}, {@link Placement#of},
 * {@link Fit#check}. It decides nothing itself. Sizes travel in the request (version 1 stores none; DECISIONS.md,
 * WR-021). Numbers are read as {@link Number} so that a fraction or a value beyond {@code int} is a 400 naming the
 * field instead of being cut to an integer.
 */
@RestController
public class AvatarController {

    /** Request body, schema {@code DressRequest}. */
    public record DressRequest(Map<String, Number> measurements, List<DressItem> items) {}

    /** One item of the request, schema {@code DressItem}; {@code size} may be absent. */
    public record DressItem(String id, Map<String, Number> size) {}

    /** Answer, schema {@code DressedAvatar}. */
    public record DressedAvatar(FigureView figure, List<LayerView> layers) {}

    /** Schema {@code Figure}: the landmarks and half-widths of {@link Figure}. */
    public record FigureView(Map<String, Integer> landmarks, Map<String, Integer> halfWidths) {
        static FigureView of(Figure f) {
            Map<String, Integer> landmarks = new LinkedHashMap<>();
            landmarks.put("head", f.head());
            landmarks.put("neck", f.neck());
            landmarks.put("shoulders", f.shoulders());
            landmarks.put("chest", f.chest());
            landmarks.put("waist", f.waist());
            landmarks.put("hips", f.hips());
            landmarks.put("crotch", f.crotch());
            landmarks.put("knee", f.knee());
            landmarks.put("ankle", f.ankle());
            landmarks.put("floor", f.floor());
            Map<String, Integer> halfWidths = new LinkedHashMap<>();
            halfWidths.put("shoulders", f.shoulderHalfWidth());
            halfWidths.put("chest", f.chestHalfWidth());
            halfWidths.put("waist", f.waistHalfWidth());
            halfWidths.put("hips", f.hipHalfWidth());
            return new FigureView(landmarks, halfWidths);
        }
    }

    /** Schema {@code Shape}; {@code sleeveEnd} null is left out of the JSON. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ShapeView(int top, int bottom, int topHalfWidth, int bottomHalfWidth, Integer sleeveEnd,
            boolean clamped) {
        static ShapeView of(Placement.Shape s) {
            return new ShapeView(s.top(), s.bottom(), s.topHalfWidth(), s.bottomHalfWidth(),
                    s.sleeveEnd().isPresent() ? s.sleeveEnd().getAsInt() : null, s.clamped());
        }
    }

    /** Schema {@code FitNote}. */
    public record FitNoteView(String kind, String part) {}

    /** Schema {@code Layer}; {@code shape} null (ACCESSORY) is left out of the JSON. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LayerView(String itemId, String type, ShapeView shape, List<FitNoteView> fit) {}

    private static final List<String> MEASUREMENTS =
            List.of("height", "shoulders", "chest", "waist", "hips", "arm", "inseam");

    private final ItemCatalog catalog;

    public AvatarController(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    @PostMapping("/avatar/dress")
    public DressedAvatar dress(@RequestBody DressRequest body) {
        Measurements measurements = measurements(body.measurements());
        if (body.items() == null) {
            throw new StatusController.BadRequest("items: the body must list the items (may be empty)");
        }
        List<Item> picked = new ArrayList<>();
        Map<ItemId, GarmentSize> sizes = new LinkedHashMap<>();
        for (int i = 0; i < body.items().size(); i++) {
            DressItem requested = body.items().get(i);
            String at = "items[" + i + "]";
            if (requested == null || requested.id() == null || requested.id().isBlank()) {
                throw new StatusController.BadRequest(at + ".id: every item must have an id");
            }
            GarmentSize size = size(at + ".size", requested.size());
            ItemId id = new ItemId(requested.id());
            Item item = catalog.find(id).orElseThrow(() -> new UnknownItem(id));
            picked.add(item);
            sizes.putIfAbsent(id, size); // the same item twice is two of a kind: refused by Layers below
        }
        List<Item> ordered;
        try {
            ordered = Layers.order(new Outfit(picked));
        } catch (IllegalArgumentException e) {
            throw new ImpossibleOutfit(e.getMessage());
        }
        Figure figure = Figure.of(measurements);
        List<LayerView> layers = new ArrayList<>();
        for (Item item : ordered) {
            GarmentSize size = sizes.get(item.id());
            ShapeView shape = item.type() == ItemType.ACCESSORY
                    ? null
                    : ShapeView.of(Placement.of(figure, item.type(), size));
            List<FitNoteView> fit = Fit.check(measurements, item.type(), size).stream()
                    .map(n -> new FitNoteView(n.kind().name(), n.part().name())).toList();
            layers.add(new LayerView(item.id().value(), item.type().name(), shape, fit));
        }
        return new DressedAvatar(FigureView.of(figure), layers);
    }

    private static Measurements measurements(Map<String, Number> m) {
        if (m == null) {
            throw new StatusController.BadRequest("measurements: the body must give the measurements");
        }
        int[] v = new int[MEASUREMENTS.size()];
        for (int i = 0; i < v.length; i++) {
            String field = MEASUREMENTS.get(i);
            Number n = m.get(field);
            if (n == null) {
                throw new StatusController.BadRequest("measurements." + field + ": missing");
            }
            v[i] = millimetres("measurements." + field, n);
        }
        try {
            return new Measurements(v[0], v[1], v[2], v[3], v[4], v[5], v[6]);
        } catch (InvalidMeasurement e) {
            throw new StatusController.BadRequest("measurements." + e.getMessage());
        }
    }

    private static GarmentSize size(String at, Map<String, Number> s) {
        if (s == null) {
            return GarmentSize.empty();
        }
        try {
            return new GarmentSize(optional(at, s, "chest"), optional(at, s, "waist"), optional(at, s, "hips"),
                    optional(at, s, "length"), optional(at, s, "sleeve"), optional(at, s, "inseam"));
        } catch (IllegalArgumentException e) { // a value that is not positive; the message starts with the field
            throw new StatusController.BadRequest(at + "." + e.getMessage());
        }
    }

    private static OptionalInt optional(String at, Map<String, Number> s, String field) {
        Number n = s.get(field);
        return n == null ? OptionalInt.empty() : OptionalInt.of(millimetres(at + "." + field, n));
    }

    private static int millimetres(String field, Number n) {
        if (!(n instanceof Integer i)) {
            throw new StatusController.BadRequest(field + ": " + n + " must be a whole number of millimetres");
        }
        return i;
    }

    /** The outfit cannot be put on a body; the message lists the domain's problems. */
    static final class ImpossibleOutfit extends RuntimeException {
        ImpossibleOutfit(String message) {
            super(message);
        }
    }
}
