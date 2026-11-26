package wardrobe.app.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemStatus;

/**
 * REST adapter: the item list (contract: {@code api/openapi.yaml}, {@code GET /items}), optionally only the items
 * in one status ({@code ?status=CLEAN}). A status that is not one of the four fails binding and becomes a 400 in
 * {@link ApiErrors}.
 */
@RestController
public class ItemsController {

    private final ItemCatalog catalog;

    public ItemsController(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/items")
    public List<ItemView> items(@RequestParam(name = "status", required = false) ItemStatus status) {
        return catalog.all().stream()
                .filter(item -> status == null || item.status() == status)
                .map(ItemView::of).toList();
    }
}
