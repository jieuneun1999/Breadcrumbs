package breadcrumbs.dsa.dll;

import breadcrumbs.domain.Checkpoint;
import breadcrumbs.domain.Checkpoint.Origin;
import breadcrumbs.domain.Checkpoint.Status;
import java.lang.reflect.Field;
import java.time.Instant;

public final class CheckpointListTest {
    private static int assertions;

    private CheckpointListTest() { }

    public static void run() throws Exception {
        CheckpointList list = new CheckpointList();
        check(list.isEmpty() && list.size() == 0, "empty state");
        check(list.getLatest() == null && list.getOldest() == null, "empty endpoints");
        check(list.findById("DEMO-X") == null, "empty search");
        check(!list.removeById("DEMO-X"), "empty deletion");
        check(list.displayStructure().equals("NULL (empty)"), "empty structure");
        order(list);
        list.append(cp("B"));
        order(list, "B");
        list.append(cp("D"));
        list.append(cp("F"));
        order(list, "B", "D", "F");
        list.prepend(cp("A"));
        order(list, "A", "B", "D", "F");
        list.insertAt(0, cp("Z"));
        order(list, "Z", "A", "B", "D", "F");
        list.insertAt(3, cp("C"));
        order(list, "Z", "A", "B", "C", "D", "F");
        list.insertAt(list.size(), cp("G"));
        order(list, "Z", "A", "B", "C", "D", "F", "G");
        check(list.findById("DEMO-C").title().equals("Title C"), "find middle");
        check(list.findById("DEMO-Z") == list.getOldest(), "find head");
        check(list.findById("DEMO-G") == list.getLatest(), "find tail");
        check(list.findById("DEMO-MISSING") == null, "missing search");
        rejects(() -> list.append(cp("C")), "duplicate append");
        rejects(() -> list.prepend(cp("C")), "duplicate prepend");
        rejects(() -> list.insertAt(2, cp("C")), "duplicate insert");
        rejects(() -> list.insertAt(-1, cp("X")), "negative position");
        rejects(() -> list.insertAt(list.size() + 1, cp("X")), "large position");
        rejects(() -> list.append(null), "null checkpoint");
        rejects(() -> list.findById(" "), "blank lookup");
        rejects(() -> list.removeById(null), "null lookup");
        order(list, "Z", "A", "B", "C", "D", "F", "G");
        Checkpoint[] copy = list.traverseForward();
        copy[0] = cp("X");
        order(list, "Z", "A", "B", "C", "D", "F", "G");
        check(!list.removeById("DEMO-MISSING"), "missing deletion");
        check(list.removeById("DEMO-Z"), "delete head");
        order(list, "A", "B", "C", "D", "F", "G");
        check(list.removeById("DEMO-C"), "delete middle");
        order(list, "A", "B", "D", "F", "G");
        check(list.removeById("DEMO-G"), "delete tail");
        order(list, "A", "B", "D", "F");
        for (String id : new String[] {"A", "B", "D", "F"}) {
            check(list.removeById("DEMO-" + id), "remove to empty");
            check(list.validateIntegrity(), "integrity after removal");
        }
        order(list);
        list.prepend(cp("P"));
        order(list, "P");
        check(list.removeById("DEMO-P"), "delete singleton prepend");
        list.insertAt(0, cp("I"));
        order(list, "I");
        check(list.removeById("DEMO-I"), "delete singleton insert");
        order(list);
        domain();
        corruptions();
        stress();
        System.out.println("PASS: DLL/domain (" + assertions + " assertions)");
    }

    private static void domain() {
        rejects(() -> new Checkpoint("", "title", "", Status.UNKNOWN,
                Instant.EPOCH, Origin.DEMO), "missing ID");
        rejects(() -> new Checkpoint("B0", "title", "", Status.UNKNOWN,
                Instant.EPOCH, Origin.DEMO), "baseline is not a checkpoint");
        rejects(() -> new Checkpoint("CP-000001", " ", "", Status.UNKNOWN,
                Instant.EPOCH, Origin.USER_CONFIRMED), "empty title");
        rejects(() -> new Checkpoint("DEMO-X", "title", "", null,
                Instant.EPOCH, Origin.DEMO), "missing status");
        rejects(() -> new Checkpoint("DEMO-X", "title", "", Status.UNKNOWN,
                null, Origin.DEMO), "missing timestamp");
        rejects(() -> new Checkpoint("DEMO-X", "title", "", Status.UNKNOWN,
                Instant.EPOCH, null), "missing origin");
        rejects(() -> new Checkpoint("CP-000000", "title", "", Status.UNKNOWN,
                Instant.EPOCH, Origin.USER_CONFIRMED), "zero native ID");
        rejects(() -> new Checkpoint("CP-000001", "title", "", Status.UNKNOWN,
                Instant.EPOCH, Origin.DEMO), "origin mismatch");
        rejects(() -> new Checkpoint("DEMO-X", "bad\u001b[31m", "", Status.UNKNOWN,
                Instant.EPOCH, Origin.DEMO), "terminal controls");
        rejects(() -> new Checkpoint("DEMO-X", "x".repeat(121), "", Status.UNKNOWN,
                Instant.EPOCH, Origin.DEMO), "bounded title");
        Checkpoint nativeCp = new Checkpoint("CP-000001", " First ", " ",
                Status.UNKNOWN, Instant.EPOCH, Origin.USER_CONFIRMED);
        check(nativeCp.summary().equals("UNKNOWN"), "missing summary stays unknown");
        check(nativeCp.title().equals("First"), "trim title");
    }

    private static void corruptions() throws Exception {
        CheckpointList list = new CheckpointList();
        list.append(cp("A"));
        list.append(cp("B"));
        list.append(cp("C"));
        Field headField = CheckpointList.class.getDeclaredField("head");
        headField.setAccessible(true);
        CheckpointNode head = (CheckpointNode) headField.get(list);
        CheckpointNode middle = head.next;
        CheckpointNode tail = middle.next;
        check(head.previous == null && tail.next == null, "null boundary pointers");
        check(middle.previous == head && tail.previous == middle, "back links");
        tail.next = head;
        check(!list.validateIntegrity(), "detect forward cycle");
        tail.next = null;
        head.previous = tail;
        check(!list.validateIntegrity(), "detect backward cycle");
        head.previous = null;
        middle.previous = null;
        check(!list.validateIntegrity(), "detect broken reciprocal link");
        middle.previous = head;
        middle.next = middle;
        check(!list.validateIntegrity(), "detect interior forward cycle");
        middle.next = tail;
        middle.previous = tail;
        check(!list.validateIntegrity(), "detect interior backward cycle");
        middle.previous = head;
        CheckpointNode duplicate = new CheckpointNode(cp("A"));
        duplicate.previous = middle;
        middle.next = duplicate;
        Field tailField = CheckpointList.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        tailField.set(list, duplicate);
        check(!list.validateIntegrity(), "detect duplicate IDs in corrupted chain");
        middle.next = tail;
        tailField.set(list, tail);
        Field sizeField = CheckpointList.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        sizeField.setInt(list, 2);
        check(!list.validateIntegrity(), "detect incorrect size");
        sizeField.setInt(list, 3);
        check(list.validateIntegrity(), "restored fixture valid");
    }

    private static void stress() {
        CheckpointList list = new CheckpointList();
        String[] expected = new String[80];
        int count = 0;
        for (int i = 0; i < 80; i++) {
            int position = (i * 17) % (count + 1);
            for (int j = count; j > position; j--) {
                expected[j] = expected[j - 1];
            }
            expected[position] = "S" + i;
            list.insertAt(position, cp(expected[position]));
            count++;
            check(list.validateIntegrity(), "mixed insertion invariant");
            for (int j = 0; j < count; j++) {
                check(list.traverseForward()[j].checkpointId().equals("DEMO-" + expected[j]),
                        "mixed insertion order");
            }
        }
        for (int i = 0; i < 80; i++) {
            check(list.removeById("DEMO-S" + i), "mixed removal");
            check(list.size() == 79 - i && list.validateIntegrity(), "mixed removal invariant");
        }
    }

    private static Checkpoint cp(String suffix) {
        return new Checkpoint("DEMO-" + suffix, "Title " + suffix, "Summary " + suffix,
                Status.IN_PROGRESS, Instant.EPOCH, Origin.DEMO);
    }

    private static void order(CheckpointList list, String... ids) {
        check(list.validateIntegrity(), "integrity");
        check(list.size() == ids.length && list.isEmpty() == (ids.length == 0), "size");
        Checkpoint[] forward = list.traverseForward();
        Checkpoint[] backward = list.traverseBackward();
        check(forward.length == ids.length && backward.length == ids.length, "traversal counts");
        for (int i = 0; i < ids.length; i++) {
            check(forward[i].checkpointId().equals("DEMO-" + ids[i]), "forward order");
            check(backward[i].checkpointId().equals("DEMO-" + ids[ids.length - i - 1]),
                    "backward order");
        }
        if (ids.length > 0) {
            check(list.getOldest() == forward[0], "oldest");
            check(list.getLatest() == forward[ids.length - 1], "latest");
        }
    }

    private static void rejects(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            assertions++;
            return;
        }
        throw new AssertionError(message);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
