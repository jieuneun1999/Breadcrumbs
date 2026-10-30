package breadcrumbs;

import breadcrumbs.cli.ConsoleMenuTest;
import breadcrumbs.dsa.dll.CheckpointListTest;

public final class AllTests {
    private AllTests() { }

    public static void main(String[] args) throws Exception {
        CheckpointListTest.run();
        ConsoleMenuTest.run();
        System.out.println("PASS: all B1 test groups");
    }
}
