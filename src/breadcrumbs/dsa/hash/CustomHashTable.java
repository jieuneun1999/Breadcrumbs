package breadcrumbs.dsa.hash;

/** Separate chaining, deterministic spreading and manual rehashing. Null keys/values are rejected. */
public final class CustomHashTable<V> {
    private HashEntry<V>[] buckets;
    private int size;
    private final int maximumEntries;
    /**
     * Creates the default empty buckets. Time: O(b) allocation. Space: O(b).
     */
    public CustomHashTable() { this(4, 50000); }
    /**
     * Allocates b power-of-two buckets with a bounded entry count.
     * Time: O(b). Auxiliary/storage space: O(b).
     */
    @SuppressWarnings("unchecked")
    public CustomHashTable(int capacity, int maximumEntries) {
        if (capacity < 2 || capacity > 131072 || Integer.bitCount(capacity) != 1 || maximumEntries < 1 || maximumEntries > 50000) {
            throw new IllegalArgumentException("Power-of-two capacity 2..131072 and bounded entry count required.");
        }
        buckets = (HashEntry<V>[]) new HashEntry<?>[capacity]; this.maximumEntries = maximumEntries;
    }
    /**
     * Returns stored entry count. Time: O(1). Auxiliary space: O(1).
     */
    public int size() { return size; }
    /**
     * Returns bucket count. Time: O(1). Auxiliary space: O(1).
     */
    public int capacity() { return buckets.length; }
    private static void key(String key) {
        if (key == null || key.length() > 8192) { throw new IllegalArgumentException("Non-null bounded key required."); }
    }
    /**
     * Spreads the String hash into a valid bucket. Time: O(L) hashing worst case. Space: O(1).
     */
    public int bucketIndex(String key) {
        key(key); int hash = key.hashCode();
        // Masking after high-bit spreading handles negative hashes, including MIN_VALUE.
        return (hash ^ (hash >>> 16)) & (buckets.length - 1);
    }
    /**
     * Scans one manual collision chain; null means absent.
     * Time: expected O(L), worst O(L*h) for length-L keys and h entries. Space: O(1).
     */
    public V get(String key) {
        for (HashEntry<V> entry = buckets[bucketIndex(key)]; entry != null; entry = entry.next) {
            if (entry.key.equals(key)) { return entry.value; }
        }
        return null;
    }
    /**
     * Tests membership through get. Time: expected O(L), worst O(L*h). Space: O(1).
     */
    public boolean containsKey(String key) { return get(key) != null; }
    /**
     * Updates a key or inserts a node; rehashes above 75% load.
     * Time: amortized expected O(L); worst O(L*h+b) collision/resize work.
     * Auxiliary space: O(b) on resize, O(1) otherwise, plus one stored entry.
     */
    public void put(String key, V value) {
        key(key); if (value == null) { throw new IllegalArgumentException("Null values are not supported."); }
        int bucket = bucketIndex(key);
        for (HashEntry<V> entry = buckets[bucket]; entry != null; entry = entry.next) {
            if (entry.key.equals(key)) { entry.value = value; return; }
        }
        if (size == maximumEntries) { throw new IllegalArgumentException("Hash table entry capacity reached."); }
        if (size + 1 > buckets.length * .75) { resize(); bucket = bucketIndex(key); }
        buckets[bucket] = new HashEntry<>(key, value, buckets[bucket]); size++;
    }
    @SuppressWarnings("unchecked")
    private void resize() {
        if (buckets.length == 131072) { throw new IllegalArgumentException("Maximum bucket capacity reached."); }
        HashEntry<V>[] old = buckets;
        buckets = (HashEntry<V>[]) new HashEntry<?>[old.length * 2];
        for (HashEntry<V> chain : old) {
            while (chain != null) {
                HashEntry<V> next = chain.next;
                int bucket = bucketIndex(chain.key); chain.next = buckets[bucket]; buckets[bucket] = chain; chain = next;
            }
        }
    }
    /**
     * Unlinks a matching bucket entry or returns null.
     * Time: expected O(L), worst O(L*h). Auxiliary space: O(1).
     */
    public V remove(String key) {
        int bucket = bucketIndex(key); HashEntry<V> previous = null;
        for (HashEntry<V> entry = buckets[bucket]; entry != null; entry = entry.next) {
            if (entry.key.equals(key)) {
                if (previous == null) { buckets[bucket] = entry.next; } else { previous.next = entry.next; }
                size--; return entry.value;
            }
            previous = entry;
        }
        return null;
    }
    /**
     * Clears buckets, retaining capacity. Time: O(b). Auxiliary space: O(1).
     */
    public void clear() { java.util.Arrays.fill(buckets, null); size = 0; }
    /**
     * Checks cycles, bucket placement, count and pairwise duplicate keys.
     * Time: O(b+L*h^2) worst case. Auxiliary space: O(1).
     */
    public boolean validate() {
        int count = 0;
        for (int i = 0; i < buckets.length; i++) {
            HashEntry<V> slow = buckets[i], fast = slow;
            while (fast != null && fast.next != null) {
                slow = slow.next; fast = fast.next.next; if (slow == fast) { return false; }
            }
            for (HashEntry<V> entry = buckets[i]; entry != null; entry = entry.next) {
                if (++count > size || entry.value == null || bucketIndex(entry.key) != i) { return false; }
                for (HashEntry<V> other = entry.next; other != null; other = other.next) {
                    if (entry.key.equals(other.key)) { return false; }
                }
            }
        }
        return count == size;
    }
    /**
     * Renders sanitized, bounded bucket chains for the DSA defense.
     * Time: O(b+h*L) upper bound. Auxiliary space: capped output, excluding source entries.
     */
    public String displayBuckets() {
        StringBuilder out = new StringBuilder("entries=" + size + " buckets=" + buckets.length + "\n");
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] == null) { continue; }
            out.append(i).append(':'); int count = 0;
            for (HashEntry<V> entry = buckets[i]; entry != null; entry = entry.next) {
                if (++count > size || out.length() > 16000) { return out.append(" [display limited]\n").toString(); }
                out.append(' ').append(breadcrumbs.inspector.ContentSafety.hasSecretMarker(entry.key) ? "[REDACTED]"
                        : breadcrumbs.inspector.ContentSafety.terminal(entry.key, 80));
            }
            out.append(" -> NULL\n");
        }
        return out.toString();
    }
}
