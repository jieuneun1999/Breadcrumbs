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
        breadcrumbs.storage.ContinuityTest.run();
        breadcrumbs.cli.ProjectConsoleTest.run();
        breadcrumbs.cli.StartupRendererTest.run();
        System.out.println("PASS: all B3 test groups");
        breadcrumbs.b4.AuditPolicyTest.run();
        breadcrumbs.b4.DriftTest.run();
        breadcrumbs.b4.WorkflowTest.run();
        breadcrumbs.b4.ContextConsoleTest.run();
        System.out.println("PASS: all B4 test groups (" + breadcrumbs.b4.Checks.count() + " assertions)");
        int b5Start = breadcrumbs.b4.Checks.count();
        breadcrumbs.b5.GraphTest.run();
        breadcrumbs.b5.GraphCliTest.run();
        breadcrumbs.b5.HashTest.run();
        breadcrumbs.b5.IndexTest.run();
        breadcrumbs.b5.HashCliTest.run();
        breadcrumbs.b5.CorruptionTest.run();
        System.out.println("PASS: all B5 groups (" + (breadcrumbs.b4.Checks.count() - b5Start) + " assertions)");
        int b6Start = breadcrumbs.b4.Checks.count();
        breadcrumbs.b6.TrieTest.run();
        breadcrumbs.b6.SearchTest.run();
        breadcrumbs.b6.BoundsTest.run();
        breadcrumbs.b6.SearchCliTest.run();
        System.out.println("PASS: all B6 groups (" + (breadcrumbs.b4.Checks.count() - b6Start) + " assertions)");
        int b7Start = breadcrumbs.b4.Checks.count();
        breadcrumbs.storage.B7RecoveryTest.run();
        System.out.println("PASS: B7A recovery (" + (breadcrumbs.b4.Checks.count() - b7Start) + " assertions)");
        int b7bStart = breadcrumbs.b4.Checks.count();
        breadcrumbs.cli.OperationFeedbackTest.run();
        breadcrumbs.b7.ContinuityWorkflowTest.run();
        System.out.println("PASS: B7B continuity and terminal feedback (" + (breadcrumbs.b4.Checks.count() - b7bStart) + " assertions)");
    }
}
