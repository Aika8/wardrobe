package wardrobe.app.web;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.domain.Item;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemId;
import wardrobe.domain.UnknownItem;
import wardrobe.domain.match.CombinationRule;
import wardrobe.domain.outfit.Explain;
import wardrobe.domain.outfit.Outfit;
import wardrobe.domain.outfit.OutfitCheck;
import wardrobe.domain.outfit.OutfitProblem;
import wardrobe.domain.outfit.WhyLine;

/**
 * REST adapter: whether an outfit can be worn, and why (contract: {@code api/openapi.yaml}, {@code POST
 * /outfits/check}). Reads the ids into an {@link Outfit} and calls the domain: {@link Explain#outfit} for the lines
 * and {@link OutfitCheck#check} (with the server's combination rule, {@link wardrobe.app.MatchConfig}) for the
 * problems. It decides nothing itself: {@code ok} is "no problems", and each problem is only given a code by its kind.
 */
@RestController
public class OutfitCheckController {

    /** Request body, schema {@code OutfitCheckRequest}. */
    public record CheckRequest(List<String> items) {}

    /** Schema {@code WhyLine}. */
    public record WhyLineView(String rule, boolean goes, String code, List<String> items, String reason) {
        static WhyLineView of(WhyLine l) {
            return new WhyLineView(l.rule().name(), l.goes(), l.code().name(),
                    l.items().stream().map(ItemId::value).toList(), l.reason());
        }
    }

    /** Schema {@code OutfitProblem}. */
    public record ProblemView(String code, String message, List<String> items) {
        static ProblemView of(OutfitProblem p) {
            return switch (p) {
                case OutfitProblem.NotClean nc -> view("NOT_CLEAN", p, nc.item());
                case OutfitProblem.TwoOfAKind t -> view("TWO_OF_A_KIND", p, t.a(), t.b());
                case OutfitProblem.DressWithSeparates d -> view("DRESS_WITH_SEPARATES", p, d.dress(), d.other());
                case OutfitProblem.Clash c -> view("CLASH", p, c.a(), c.b());
                // An empty list is refused with 400 before the domain is asked.
                case OutfitProblem.Empty e -> throw new IllegalStateException("an empty outfit reached the check");
            };
        }

        private static ProblemView view(String code, OutfitProblem p, Item... items) {
            List<String> ids = new ArrayList<>();
            for (Item item : items) {
                ids.add(item.id().value());
            }
            return new ProblemView(code, p.message(), ids);
        }
    }

    /** Answer, schema {@code OutfitVerdict}. */
    public record OutfitVerdict(boolean ok, List<WhyLineView> why, List<ProblemView> problems) {}

    private final ItemCatalog catalog;
    private final CombinationRule combination;

    public OutfitCheckController(ItemCatalog catalog, CombinationRule combination) {
        this.catalog = catalog;
        this.combination = combination;
    }

    @PostMapping("/outfits/check")
    public OutfitVerdict check(@RequestBody CheckRequest body) {
        if (body.items() == null || body.items().isEmpty()) {
            throw new StatusController.BadRequest("items: the body must list at least one item id");
        }
        List<Item> picked = new ArrayList<>();
        for (int i = 0; i < body.items().size(); i++) {
            String id = body.items().get(i);
            if (id == null || id.isBlank()) {
                throw new StatusController.BadRequest("items[" + i + "]: every item id must be a non-blank string");
            }
            ItemId itemId = new ItemId(id);
            picked.add(catalog.find(itemId).orElseThrow(() -> new UnknownItem(itemId)));
        }
        Outfit outfit = new Outfit(picked);
        List<ProblemView> problems = OutfitCheck.check(outfit, combination).stream().map(ProblemView::of).toList();
        List<WhyLineView> why = Explain.outfit(outfit).stream().map(WhyLineView::of).toList();
        return new OutfitVerdict(problems.isEmpty(), why, problems);
    }
}
