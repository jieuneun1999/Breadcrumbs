package breadcrumbs.b4;

/** Fast affected-feature loop; AllTests also runs every B1–B3 suite without weakening it. */
public final class B4Tests {
    private B4Tests() { }
    public static void main(String[] args) throws Exception {
        AuditPolicyTest.run(); DriftTest.run(); WorkflowTest.run(); ContextConsoleTest.run();
        System.out.println("PASS: all B4 test groups (" + Checks.count() + " assertions)");
    }
}
