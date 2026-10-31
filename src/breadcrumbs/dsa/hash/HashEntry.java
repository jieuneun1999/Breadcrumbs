package breadcrumbs.dsa.hash;

/** Bucket-chain link, private to the manually implemented table. */
final class HashEntry<V> {
    final String key;
    V value;
    HashEntry<V> next;
    HashEntry(String key, V value, HashEntry<V> next) { this.key = key; this.value = value; this.next = next; }
}
