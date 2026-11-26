package wardrobe.app.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import wardrobe.domain.IllegalStatusChange;
import wardrobe.domain.UnknownItem;

/**
 * Domain and request errors as the contract's {@code Error {code, message}}: unknown item → 404, a change the
 * status table refuses → 409, an unreadable body or a status (in the body or the query) that is not one of the four
 * → 400. The 409 message is the domain's own ("item X cannot go from A to B"), so the wording lives in one place.
 */
@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(UnknownItem.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorView unknownItem(UnknownItem e) {
        return new ErrorView(ErrorView.UNKNOWN_ITEM, "no item with id " + e.id());
    }

    @ExceptionHandler(IllegalStatusChange.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorView illegalStatusChange(IllegalStatusChange e) {
        return new ErrorView(ErrorView.ILLEGAL_STATUS_CHANGE, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorView unreadable(HttpMessageNotReadableException e) {
        return new ErrorView(ErrorView.BAD_REQUEST,
                "the body must be JSON like {\"status\": \"IN_WASH\"} with one of CLEAN, IN_WASH, IN_REPAIR, ON_TRIP");
    }

    @ExceptionHandler(StatusController.BadRequest.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorView badRequest(StatusController.BadRequest e) {
        return new ErrorView(ErrorView.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorView badParameter(MethodArgumentTypeMismatchException e) {
        return new ErrorView(ErrorView.BAD_REQUEST,
                "the parameter " + e.getName() + " must be one of CLEAN, IN_WASH, IN_REPAIR, ON_TRIP");
    }
}
