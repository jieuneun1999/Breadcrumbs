package breadcrumbs.search;

final class PostingList {
    PostingNode first, last;
    int size;
    boolean add(int field, int maximum) {
        if (last != null && last.field == field) { last.occurrences++; return false; }
        if (size == maximum) { throw new IllegalArgumentException("Per-term posting capacity exceeded."); }
        PostingNode node = new PostingNode(field);
        if (first == null) { first = node; } else { last.next = node; }
        last = node; size++; return true;
    }
    boolean validate(int fields) {
        int count = 0, previous = -1; PostingNode tail = null;
        for (PostingNode node = first; node != null; node = node.next) {
            if (++count > size || node.field <= previous || node.field >= fields || node.occurrences < 1) { return false; }
            previous = node.field; tail = node;
        }
        return count == size && tail == last && size > 0;
    }
}
