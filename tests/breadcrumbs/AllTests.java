package breadcrumbs;

import breadcrumbs.cli.ConsoleMenuTest;
import breadcrumbs.dsa.dll.CheckpointListTest;
import breadcrumbs.inspector.ProjectInspectorTest;
import breadcrumbs.bootstrap.BootstrapTest;
import breadcrumbs.cli.BootstrapConsoleTest;

public final class AllTests {
    private AllTests() { }

    public static void main(String[] args) throws Exception {
        CheckpointListTest.run();
        ConsoleMenuTest.run();
        System.out.println("PASS: all B1 test groups");
        ProjectInspectorTest.run();
        BootstrapTest.run();
        BootstrapConsoleTest.run();
        System.out.println("PASS: all B2 test groups");
    }
}
