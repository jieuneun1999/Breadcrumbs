package breadcrumbs.search;

/** Field slot belongs exclusively to the candidate's scoped locations and revision. */
final class PostingNode {
    final int field;
    int occurrences = 1;
    PostingNode next;
    PostingNode(int field) { this.field = field; }
}
