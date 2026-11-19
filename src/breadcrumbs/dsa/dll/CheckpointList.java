package breadcrumbs.dsa.dll;

import breadcrumbs.domain.Checkpoint;

public final class CheckpointList {
    private CheckpointNode head;
    private CheckpointNode tail;
    private int size;
    private long modification;
    /**
     * Returns the mutation revision used to invalidate disposable indexes.
     * Time: O(1). Auxiliary space: O(1).
     */
    public long modification() { return modification; }

    /**
     * Adds a unique checkpoint at TAIL.
     * Time: O(n) duplicate-ID search; tail linking is O(1). Auxiliary space: O(1).
     */
    public void append(Checkpoint checkpoint) {
        // Finding a duplicate is O(n); the following tail-link changes are O(1).
        requireUnique(checkpoint);
        CheckpointNode node = new CheckpointNode(checkpoint);
        node.previous = tail;
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
        modification++;
    }

    /**
     * Adds a unique checkpoint at HEAD and repairs its previous link.
     * Time: O(n) duplicate-ID search; head linking is O(1). Auxiliary space: O(1).
     */
    public void prepend(Checkpoint checkpoint) {
        requireUnique(checkpoint);
        CheckpointNode node = new CheckpointNode(checkpoint);
        node.next = head;
        // Empty insertion sets both endpoints. Otherwise the former head must point back to this node.
        if (head == null) {
            tail = node;
        } else {
            head.previous = node;
        }
        head = node;
        size++;
        modification++;
    }

    /**
     * Inserts at a zero-based position in [0, size], including both endpoints.
     * Time: O(n) uniqueness/position search. Auxiliary space: O(1).
     */
    public void insertAt(int index, Checkpoint checkpoint) {
        if (index < 0 || index > size) {
            throw new IllegalArgumentException("Position must be between 0 and " + size + ".");
        }
        if (index == 0) {
            prepend(checkpoint);
            return;
        }
        if (index == size) {
            append(checkpoint);
            return;
        }
        requireUnique(checkpoint);
        CheckpointNode following = head;
        for (int i = 0; i < index; i++) {
            following = following.next;
        }
        CheckpointNode node = new CheckpointNode(checkpoint);
        node.previous = following.previous;
        node.next = following;
        // Splice between two existing nodes: repair both neighbors before increasing the count.
        following.previous.next = node;
        following.previous = node;
        size++;
        modification++;
    }

    /**
     * Returns a matching checkpoint, or null without changing links.
     * Time: O(n) bounded ID comparisons. Auxiliary space: O(1).
     */
    public Checkpoint findById(String checkpointId) {
        CheckpointNode node = findNode(checkpointId);
        return node == null ? null : node.checkpoint;
    }

    /**
     * Finds an ID, repairs both neighbors and detaches the removed node.
     * Time: O(n) search, then O(1) unlinking. Auxiliary space: O(1).
     */
    public boolean removeById(String checkpointId) {
        CheckpointNode node = findNode(checkpointId);
        if (node == null) {
            return false;
        }
        // Each missing neighbor identifies an endpoint; interior removal repairs the two surviving links.
        if (node.previous == null) {
            head = node.next;
        } else {
            node.previous.next = node.next;
        }
        if (node.next == null) {
            tail = node.previous;
        } else {
            node.next.previous = node.previous;
        }
        // Detach the removed node so no stale link continues to represent active list membership.
        node.previous = null;
        node.next = null;
        size--;
        modification++;
        return true;
    }

    /**
     * Returns references from HEAD to TAIL without changing nodes.
     * Time: O(n). Auxiliary/result space: O(n) for the returned array.
     */
    public Checkpoint[] traverseForward() {
        Checkpoint[] result = new Checkpoint[size];
        CheckpointNode node = head;
        for (int i = 0; i < size; i++) {
            result[i] = node.checkpoint;
            node = node.next;
        }
        return result;
    }

    /**
     * Returns references from TAIL to HEAD through previous links.
     * Time: O(n). Auxiliary/result space: O(n) for the returned array.
     */
    public Checkpoint[] traverseBackward() {
        Checkpoint[] result = new Checkpoint[size];
        CheckpointNode node = tail;
        for (int i = 0; i < size; i++) {
            result[i] = node.checkpoint;
            node = node.previous;
        }
        return result;
    }

    /**
     * Reads TAIL or null when empty; latest means list position, not timestamp.
     * Time: O(1). Auxiliary space: O(1).
     */
    public Checkpoint getLatest() {
        return tail == null ? null : tail.checkpoint;
    }

    /**
     * Reads HEAD or null when empty. Time: O(1). Auxiliary space: O(1).
     */
    public Checkpoint getOldest() {
        return head == null ? null : head.checkpoint;
    }

    /**
     * Returns maintained count without traversal. Time: O(1). Auxiliary space: O(1).
     */
    public int size() {
        return size;
    }

    /**
     * Tests the maintained count. Time: O(1). Auxiliary space: O(1).
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Renders ID links of an intact list; validate before untrusted diagnostics.
     * Time: O(n) for bounded IDs. Auxiliary/output space: O(n) for the chain.
     */
    public String displayStructure() {
        if (head == null) {
            return "NULL (empty)";
        }
        StringBuilder output = new StringBuilder("NULL <- ");
        for (CheckpointNode node = head; node != null; node = node.next) {
            output.append('[').append(node.checkpoint.checkpointId()).append(']');
            output.append(node.next == null ? " -> NULL" : " <-> ");
        }
        return output.toString();
    }

    /**
     * Checks endpoints, cycles, reciprocal links, count and pairwise ID uniqueness.
     * Time: O(n^2) uniqueness plus O(n) link checks. Auxiliary space: O(1).
     */
    public boolean validateIntegrity() {
        if (size < 0) {
            return false;
        }
        if (size == 0) {
            return head == null && tail == null;
        }
        if (head == null || tail == null || head.previous != null || tail.next != null) {
            return false;
        }
        CheckpointNode slow = head;
        CheckpointNode fast = head;
        // Floyd's two-speed traversal detects cycles without an auxiliary collection.
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                return false;
            }
        }
        int count = 0;
        CheckpointNode previous = null;
        for (CheckpointNode node = head; node != null; node = node.next) {
            if (node.previous != previous || node.checkpoint == null || ++count > size) {
                return false;
            }
            // Pairwise uniqueness makes this complete diagnostic O(n^2), despite linear link checks.
            for (CheckpointNode other = node.next; other != null; other = other.next) {
                if (other.checkpoint == null
                        || node.checkpoint.checkpointId().equals(other.checkpoint.checkpointId())) {
                    return false;
                }
            }
            previous = node;
        }
        if (count != size || previous != tail) {
            return false;
        }
        count = 0;
        CheckpointNode next = null;
        for (CheckpointNode node = tail; node != null; node = node.previous) {
            if (node.next != next || ++count > size) {
                return false;
            }
            next = node;
        }
        return count == size && next == head;
    }

    private void requireUnique(Checkpoint checkpoint) {
        if (checkpoint == null) {
            throw new IllegalArgumentException("Checkpoint must not be null.");
        }
        if (findNode(checkpoint.checkpointId()) != null) {
            throw new IllegalArgumentException("Duplicate checkpoint ID.");
        }
    }

    private CheckpointNode findNode(String checkpointId) {
        if (checkpointId == null || checkpointId.isBlank()) {
            throw new IllegalArgumentException("Identifier must not be blank.");
        }
        for (CheckpointNode node = head; node != null; node = node.next) {
            if (node.checkpoint.checkpointId().equals(checkpointId)) {
                return node;
            }
        }
        return null;
    }
}
