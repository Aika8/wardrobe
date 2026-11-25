package wardrobe.app.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.domain.ItemCatalog;

/** REST adapter: the item list (contract: {@code api/openapi.yaml}, {@code GET /items}). */
@RestController
public class ItemsController {

    private final ItemCatalog catalog;

    public ItemsController(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/items")
    public List<ItemView> items() {
        return catalog.all().stream().map(ItemView::of).toList();
    }
}
