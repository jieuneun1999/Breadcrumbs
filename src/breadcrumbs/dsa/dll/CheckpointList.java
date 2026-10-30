package breadcrumbs.dsa.dll;

import breadcrumbs.domain.Checkpoint;

public final class CheckpointList {
    private CheckpointNode head;
    private CheckpointNode tail;
    private int size;

    public void append(Checkpoint checkpoint) {
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
    }

    public void prepend(Checkpoint checkpoint) {
        requireUnique(checkpoint);
        CheckpointNode node = new CheckpointNode(checkpoint);
        node.next = head;
        if (head == null) {
            tail = node;
        } else {
            head.previous = node;
        }
        head = node;
        size++;
    }

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
        following.previous.next = node;
        following.previous = node;
        size++;
    }

    public Checkpoint findById(String checkpointId) {
        CheckpointNode node = findNode(checkpointId);
        return node == null ? null : node.checkpoint;
    }

    public boolean removeById(String checkpointId) {
        CheckpointNode node = findNode(checkpointId);
        if (node == null) {
            return false;
        }
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
        node.previous = null;
        node.next = null;
        size--;
        return true;
    }

    public Checkpoint[] traverseForward() {
        Checkpoint[] result = new Checkpoint[size];
        CheckpointNode node = head;
        for (int i = 0; i < size; i++) {
            result[i] = node.checkpoint;
            node = node.next;
        }
        return result;
    }

    public Checkpoint[] traverseBackward() {
        Checkpoint[] result = new Checkpoint[size];
        CheckpointNode node = tail;
        for (int i = 0; i < size; i++) {
            result[i] = node.checkpoint;
            node = node.previous;
        }
        return result;
    }

    public Checkpoint getLatest() {
        return tail == null ? null : tail.checkpoint;
    }

    public Checkpoint getOldest() {
        return head == null ? null : head.checkpoint;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

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
