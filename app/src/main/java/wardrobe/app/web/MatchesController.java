package wardrobe.app.web;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.domain.Item;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemId;
import wardrobe.domain.UnknownItem;
import wardrobe.domain.match.CombinationRule;
import wardrobe.domain.match.Verdict;

/**
 * REST adapter: what goes with one item (contract: {@code api/openapi.yaml}, {@code GET /items/{id}/matches}).
 * Every other item available for an outfit ({@link Item#availableForOutfit()}: CLEAN), in catalogue order, is
 * judged by the domain's combination rule (see {@link wardrobe.app.MatchConfig}); this class only sorts the verdicts
 * into two lists and never decides itself. The item asked about may be in any status.
 */
@RestController
public class MatchesController {

    /** One judged partner, schema {@code Match}. */
    public record MatchView(ItemView item, String reason) {}

    /** The answer, schema {@code Matches}. */
    public record MatchesView(ItemView item, List<MatchView> goes, List<MatchView> doesNot) {}

    private final ItemCatalog catalog;
    private final CombinationRule combination;

    public MatchesController(ItemCatalog catalog, CombinationRule combination) {
        this.catalog = catalog;
        this.combination = combination;
    }

    @GetMapping("/items/{id}/matches")
    public MatchesView matches(@PathVariable String id) {
        if (id.isBlank()) {
            throw new StatusController.BadRequest("the item id must not be blank");
        }
        ItemId itemId = new ItemId(id);
        List<Item> all = catalog.all();
        Item item = all.stream().filter(i -> i.id().equals(itemId)).findFirst()
                .orElseThrow(() -> new UnknownItem(itemId)); // from the same snapshot as the partners
        List<MatchView> goes = new ArrayList<>();
        List<MatchView> doesNot = new ArrayList<>();
        for (Item other : all) {
            if (other.id().equals(itemId) || !other.availableForOutfit()) {
                continue;
            }
            Verdict verdict = combination.judge(item, other);
            (verdict.goes() ? goes : doesNot).add(new MatchView(ItemView.of(other), verdict.reason()));
        }
        return new MatchesView(ItemView.of(item), goes, doesNot);
    }
}
