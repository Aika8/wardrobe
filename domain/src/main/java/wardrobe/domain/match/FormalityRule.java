package wardrobe.domain.match;

import wardrobe.domain.Item;

/** Two items combine only when their formality differs by at most one step (intent 01, invariant 4). */
public final class FormalityRule implements CombinationRule {

    static final int MAX_STEPS = 1;

    @Override
    public Verdict judge(Item a, Item b) {
        int steps = a.formality().stepsTo(b.formality());
        if (steps <= MAX_STEPS) {
            return Verdict.yes("formality " + a.formality() + " and " + b.formality() + " are close enough");
        }
        return Verdict.no("formality " + a.formality() + " and " + b.formality() + " are " + steps
                + " steps apart (at most " + MAX_STEPS + ")");
    }
}
