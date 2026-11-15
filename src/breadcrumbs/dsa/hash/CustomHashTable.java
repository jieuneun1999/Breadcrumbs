package breadcrumbs.dsa.hash;

/** Separate chaining, deterministic spreading and manual rehashing. Null keys/values are rejected. */
public final class CustomHashTable<V> {
    private HashEntry<V>[] buckets;
    private int size;
    private final int maximumEntries;
    public CustomHashTable() { this(4, 50000); }
    @SuppressWarnings("unchecked")
    public CustomHashTable(int capacity, int maximumEntries) {
        if (capacity < 2 || capacity > 131072 || Integer.bitCount(capacity) != 1 || maximumEntries < 1 || maximumEntries > 50000) {
            throw new IllegalArgumentException("Power-of-two capacity 2..131072 and bounded entry count required.");
        }
        buckets = (HashEntry<V>[]) new HashEntry<?>[capacity]; this.maximumEntries = maximumEntries;
    }
    public int size() { return size; }
    public int capacity() { return buckets.length; }
    private static void key(String key) {
        if (key == null || key.length() > 8192) { throw new IllegalArgumentException("Non-null bounded key required."); }
    }
    public int bucketIndex(String key) {
        key(key); int hash = key.hashCode();
        // Masking after high-bit spreading handles negative hashes, including MIN_VALUE.
        return (hash ^ (hash >>> 16)) & (buckets.length - 1);
    }
    public V get(String key) {
        for (HashEntry<V> entry = buckets[bucketIndex(key)]; entry != null; entry = entry.next) {
            if (entry.key.equals(key)) { return entry.value; }
        }
        return null;
    }
    public boolean containsKey(String key) { return get(key) != null; }
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
    public void clear() { java.util.Arrays.fill(buckets, null); size = 0; }
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
