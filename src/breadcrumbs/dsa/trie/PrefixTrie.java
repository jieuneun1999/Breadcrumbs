package breadcrumbs.dsa.trie;

import java.util.Arrays;

public final class PrefixTrie {
    public record Suggestions(String[] terms, boolean limited, int visited) {
        public Suggestions { terms = terms.clone(); }
        @Override public String[] terms() { return terms.clone(); }
    }
    private final int maxTerms, maxNodes, maxLength;
    private TrieNode root = new TrieNode(0, -1);
    private int terms, nodes = 1;
    public PrefixTrie(int maxTerms, int maxNodes, int maxLength) {
        if (maxTerms < 1 || maxTerms > 50000 || maxNodes < 2 || maxNodes > 500000 || maxLength < 1 || maxLength > 128) {
            throw new IllegalArgumentException("Bounded Trie capacity required.");
        }
        this.maxTerms = maxTerms; this.maxNodes = maxNodes; this.maxLength = maxLength;
    }
    private int[] points(String term) {
        if (term == null || term.isEmpty() || term.length() > maxLength * 2) { throw new IllegalArgumentException("Bounded nonempty term required."); }
        int[] points = term.codePoints().toArray();
        if (points.length > maxLength) { throw new IllegalArgumentException("Term too long."); }
        for (int point : points) {
            int type = Character.getType(point);
            if (!Character.isLetterOrDigit(point) && type != Character.NON_SPACING_MARK && type != Character.COMBINING_SPACING_MARK) {
                throw new IllegalArgumentException("Lexical term required.");
            }
        }
        return points;
    }
    private TrieNode child(TrieNode node, int point) {
        for (TrieNode edge = node.firstChild; edge != null && edge.point <= point; edge = edge.nextSibling) {
            if (edge.point == point) { return edge; }
        }
        return null;
    }
    public void insert(String term) {
        int[] points = points(term); TrieNode node = root; int matched = 0;
        while (matched < points.length) {
            TrieNode next = child(node, points[matched]); if (next == null) { break; }
            node = next; matched++;
        }
        if (matched == points.length && node.term != null) { return; }
        if (terms == maxTerms || nodes + points.length - matched > maxNodes) { throw new IllegalArgumentException("Trie capacity exceeded."); }
        while (matched < points.length) {
            int point = points[matched++]; TrieNode added = new TrieNode(nodes++, point), previous = null, edge = node.firstChild;
            while (edge != null && edge.point < point) { previous = edge; edge = edge.nextSibling; }
            added.nextSibling = edge;
            if (previous == null) { node.firstChild = added; } else { previous.nextSibling = added; }
            node = added;
        }
        node.term = term; terms++;
    }
    public boolean contains(String term) {
        TrieNode node = root;
        for (int point : points(term)) { node = child(node, point); if (node == null) { return false; } }
        return term.equals(node.term);
    }
    public int termCount() { return terms; }
    public int nodeCount() { return nodes; }
    public void clear() { root = new TrieNode(0, -1); nodes = 1; terms = 0; }
    public Suggestions suggest(String prefix, int limit, int maxVisits) {
        int[] points = points(prefix);
        if (limit < 1 || limit > 50 || maxVisits < 1 || maxVisits > 500000) { throw new IllegalArgumentException("Bounded suggestion limits required."); }
        Walk walk = new Walk(limit, maxVisits); TrieNode node = root;
        for (int point : points) {
            TrieNode edge = node.firstChild;
            while (edge != null) {
                if (!walk.visit()) { return walk.result(); }
                if (edge.point >= point) { break; } edge = edge.nextSibling;
            }
            if (edge == null || edge.point != point) { return walk.result(); } node = edge;
        }
        collect(node, walk, 0);
        return walk.result();
    }
    private void collect(TrieNode node, Walk walk, int depth) {
        if (depth > maxLength || !walk.visit()) { walk.limited = true; return; }
        if (node.term != null) {
            if (walk.count == walk.found.length) { walk.limited = true; return; }
            walk.found[walk.count++] = node.term;
        }
        for (TrieNode child = node.firstChild; child != null; child = child.nextSibling) {
            if (walk.limited) { return; } collect(child, walk, depth + 1);
        }
    }
    public boolean validate() {
        if (nodes < 1 || nodes > maxNodes || terms < 0 || terms > maxTerms || root.id != 0 || root.point != -1 || root.nextSibling != null) { return false; }
        int[] counts = new int[2]; boolean[] seen = new boolean[nodes];
        return validate(root, "", seen, counts, 0) && counts[0] == nodes && counts[1] == terms;
    }
    private boolean validate(TrieNode node, String path, boolean[] seen, int[] counts, int depth) {
        if (depth > maxLength || node.id < 0 || node.id >= nodes || seen[node.id]) { return false; }
        seen[node.id] = true; counts[0]++;
        if (node.term != null) { if (!node.term.equals(path) || depth == 0) { return false; } counts[1]++; }
        int previous = -1, siblings = 0;
        for (TrieNode child = node.firstChild; child != null; child = child.nextSibling) {
            if (++siblings > nodes || child.point <= previous || !Character.isValidCodePoint(child.point)) { return false; }
            previous = child.point;
            if (!validate(child, path + new String(Character.toChars(child.point)), seen, counts, depth + 1)) { return false; }
        }
        return true;
    }
    private static final class Walk {
        final String[] found; final int budget; int count, visited; boolean limited;
        Walk(int limit, int budget) { found = new String[limit]; this.budget = budget; }
        boolean visit() { if (visited == budget) { limited = true; return false; } visited++; return true; }
        Suggestions result() { return new Suggestions(Arrays.copyOf(found, count), limited, visited); }
    }
}
