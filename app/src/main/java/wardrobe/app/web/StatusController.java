package wardrobe.app.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.domain.ChangeStatus;
import wardrobe.domain.ItemCatalog;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;

/**
 * REST adapter: change one item's status (contract: {@code api/openapi.yaml}, {@code PUT /items/{id}/status}).
 * Whether the change is allowed is decided by the domain ({@link ChangeStatus}); errors become {@link ErrorView}
 * in {@link ApiErrors}.
 */
@RestController
public class StatusController {

    /** Request body, schema {@code StatusChange}. An unknown status name fails JSON reading and becomes a 400. */
    public record StatusChange(ItemStatus status) {}

    private final ChangeStatus changeStatus;

    public StatusController(ItemCatalog catalog) {
        this.changeStatus = new ChangeStatus(catalog);
    }

    @PutMapping("/items/{id}/status")
    public ItemView change(@PathVariable String id, @RequestBody StatusChange body) {
        if (id.isBlank()) {
            throw new BadRequest("the item id must not be blank");
        }
        if (body.status() == null) {
            throw new BadRequest("the body must name a status: one of CLEAN, IN_WASH, IN_REPAIR, ON_TRIP");
        }
        return ItemView.of(changeStatus.change(new ItemId(id), body.status()));
    }

    /** A request the contract does not accept, found after JSON reading. */
    static final class BadRequest extends RuntimeException {
        BadRequest(String message) {
            super(message);
        }
    }
}
