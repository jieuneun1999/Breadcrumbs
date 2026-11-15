package breadcrumbs.b5;

import breadcrumbs.dsa.hash.CustomHashTable;
import breadcrumbs.b4.Checks;
import static breadcrumbs.b4.Checks.*;

public final class HashTest {
    private HashTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        int initial = Checks.count();
        var table = new CustomHashTable<String>(4, 1000);
        require(table.size() == 0 && table.get("absent") == null && table.validate(), "empty");
        reject(() -> table.put(null, "value"), "null key"); reject(() -> table.put("key", null), "null value");
        reject(() -> table.get(null), "null lookup");
        table.put("Aa", "a"); table.put("BB", "b");
        require(table.bucketIndex("Aa") == table.bucketIndex("BB") && table.size() == 2, "forced collision chain");
        table.put("Aa", "updated"); require(table.size() == 2 && table.get("Aa").equals("updated"), "duplicate-key update");
        table.put("third", "3"); require(table.capacity() == 4, "0.75 threshold inclusive");
        table.put("fourth", "4"); require(table.capacity() == 8, "resize on exceeding threshold");
        String min = "polygenelubricants"; require(min.hashCode() == Integer.MIN_VALUE, "actual MIN_VALUE hash fixture");
        table.put(min, "min"); table.put("negative-hash-value", "negative");
        for (int i = 0; i < 500; i++) { table.put("K" + i, "V" + i); }
        for (int i = 0; i < 500; i++) { require(table.get("K" + i).equals("V" + i), "lookup after rehash " + i); }
        require(table.get(min).equals("min") && table.validate(), "negative hash and full chain validation");
        require(table.remove("Aa").equals("updated") && table.get("BB").equals("b"), "collision removal preserves neighbor");
        require(table.remove("absent") == null, "missing deletion");
        for (int i = 0; i < 500; i++) { require(table.remove("K" + i).equals("V" + i), "remove after resize"); }
        table.clear(); require(table.size() == 0 && table.validate() && !table.containsKey(min), "clear");
        for (int i = 0; i < 200; i++) { table.put("repeat", "v"); table.remove("repeat"); }
        require(table.size() == 0 && table.validate(), "repeated insert/delete");
        var bounded = new CustomHashTable<String>(2, 1); bounded.put("one", "v");
        reject(() -> bounded.put("two", "v"), "bounded entries"); bounded.put("one", "updated"); require(bounded.size() == 1, "update at capacity");
        reject(() -> new CustomHashTable<String>(3, 1), "invalid capacity");
        System.out.println("PASS: B5B custom Hash (" + (Checks.count() - initial) + " assertions)");
    }
}
