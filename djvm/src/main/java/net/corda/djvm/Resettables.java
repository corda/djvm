package net.corda.djvm;

import java.util.LinkedList;
import java.util.List;

final class Resettables {
    private final LinkedList<Resettable> resettables;

    Resettables() {
        resettables = new LinkedList<>();
    }

    synchronized void add(Resettable resettable) {
        resettables.add(resettable);
    }

    @SuppressWarnings("unchecked")
    synchronized List<Resettable> getResettables() {
        // Create a snapshot of the resettables that we can iterate
        // over without risking a concurrent modification exception.
        // Cloning a LinkedList is a trivial O(1) operation, whereas
        // copy-constructing it would be O(N).
        //
        // We are assuming that the only possible modification would
        // be some new items being added to the end. But this would
        // only be likely during interactive debugging anyway.
        return (List<Resettable>) resettables.clone();
    }
}
