package breadcrumbs.dsa.trie;

/** A sorted code-point edge is owned by its parent's manual child/sibling chain. */
final class TrieNode {
    final int id;
    final int point;
    TrieNode firstChild;
    TrieNode nextSibling;
    String term;
    TrieNode(int id, int point) { this.id = id; this.point = point; }
}
