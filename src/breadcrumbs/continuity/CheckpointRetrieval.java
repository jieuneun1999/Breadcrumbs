package breadcrumbs.continuity;

import breadcrumbs.domain.*;

/** Derived bounded ranking array; the custom DLL remains the only native checkpoint sequence. */
public final class CheckpointRetrieval {
    public record Ranked(Checkpoint checkpoint, double score, int sharedEvidence, double freshnessBonus, double recency) { }
    public Ranked[] rank(WorkspaceState state, String[] queryEvidence) {
        if (queryEvidence == null || queryEvidence.length > 2000) { throw new IllegalArgumentException("Bounded evidence query required."); }
        for (int i = 0; i < queryEvidence.length; i++) {
            boolean found = false;
            if (queryEvidence[i] == null) { throw new IllegalArgumentException("Invalid evidence query."); }
            for (String observation : observationIds(state)) { found |= state.evidence(observation, queryEvidence[i]) != null; }
            if (!found) { throw new IllegalArgumentException("Query evidence belongs to another workspace or does not exist."); }
            for (int j = 0; j < i; j++) { if (queryEvidence[i].equals(queryEvidence[j])) { throw new IllegalArgumentException("Duplicate query evidence."); } }
        }
        Checkpoint[] chronological = state.checkpoints().traverseForward();
        Ranked[] ranks = new Ranked[chronological.length];
        for (int i = 0; i < chronological.length; i++) {
            Checkpoint cp = chronological[i];
            if (cp.origin() != Checkpoint.Origin.USER_CONFIRMED || cp.project() == null
                    || !cp.project().workspaceId().equals(state.activeBaseline().workspaceId())) {
                throw new IllegalArgumentException("Native workspace history required; demo history is isolated.");
            }
            int overlap = 0;
            for (String ref : cp.project().evidenceIds()) {
                for (String query : queryEvidence) { if (ref.equals(query)) { overlap++; } }
            }
            double fresh = new ReferenceFreshness().assess(state, cp).state() == ReferenceFreshness.State.FRESH ? 2 : 0;
            double recency = (i + 1.0) / chronological.length;
            ranks[i] = new Ranked(cp, (overlap > 0 ? 3 : 0) + fresh + recency, overlap, fresh, recency);
        }
        // Insertion sort changes only this disposable projection, never the DLL's pointers or order.
        for (int i = 1; i < ranks.length; i++) {
            Ranked item = ranks[i]; int j = i - 1;
            while (j >= 0 && better(item, ranks[j])) { ranks[j + 1] = ranks[j--]; }
            ranks[j + 1] = item;
        }
        return ranks;
    }
    private static boolean better(Ranked a, Ranked b) {
        int score = Double.compare(a.score(), b.score());
        if (score != 0) { return score > 0; }
        int time = a.checkpoint().createdAt().compareTo(b.checkpoint().createdAt());
        return time != 0 ? time > 0 : a.checkpoint().checkpointId().compareTo(b.checkpoint().checkpointId()) > 0;
    }
    private static String[] observationIds(WorkspaceState state) {
        var later = state.laterObservations(); String[] ids = new String[later.length + 1];
        ids[0] = state.activeBaseline().snapshot().scanId().isEmpty() ? "B0-NO-SCAN" : state.activeBaseline().snapshot().scanId();
        for (int i = 0; i < later.length; i++) { ids[i + 1] = later[i].scanId(); }
        return ids;
    }
}
