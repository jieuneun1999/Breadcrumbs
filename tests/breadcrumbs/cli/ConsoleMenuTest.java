package breadcrumbs.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public final class ConsoleMenuTest {
    private static int assertions;

    private ConsoleMenuTest() { }

    public static void run() throws Exception {
        contains(session("oops\n99\n\n0\n"), "Invalid menu option", "Goodbye");
        contains(session("4\n\n5\n\n0\n"), "Identifier must not be blank");
        contains(session("3\n-1\n3\n1\n3\noops\n0\n"), "Position must be between 0 and 0");
        contains(session("1\n\n0\n"), "Title must not be blank", "NULL (empty)");
        contains(session("1\nTitle\n\nwrong\n0\n"), "Invalid status", "NULL (empty)");
        contains(session("1\nTitle\n\nUNKNOWN\nn\n10\n0\n"), "Cancelled", "Size: 0");
        contains(session("1\nTitle\n\nUNKNOWN\nyes\n8\n5\nCP-000001\nyes\n"
                + "1\nNext\nSummary\nCOMPLETED\nyes\n6\n0\n"),
                "USER_CONFIRMED", "CP-000001", "CP-000002", "Deleted CP-000001");
        contains(session("12\n12\n10\n11\n0\n"), "Loaded 5", "Loaded 0", "skipped 5",
                "Size: 5", "Integrity valid");
        contains(session("4\nDEMO-MISSING\n5\nDEMO-MISSING\n6\n7\n8\n9\n10\n11\n0\n"),
                "Not found", "No checkpoints", "No latest checkpoint", "Size: 0", "Integrity valid");
        contains(session("2\nDEMO-X\nSynthetic\nSummary\nPLANNED\nyes\n"
                + "2\nDEMO-X\nDuplicate\nSummary\nUNKNOWN\nyes\n"
                + "3\n1\nDEMO-Y\nTail\nSummary\nBLOCKED\nyes\n7\n0\n"),
                "Duplicate checkpoint ID", "[DEMO-X] <-> [DEMO-Y]", "Traverse backward");
        contains(session("2\nB0\nInvalid\n\nUNKNOWN\n0\n"), "DEMO-", "NULL (empty)");
        contains(session("1\nbad\u001b[31m\n0\n"), "control characters");
        contains(session("x".repeat(1100) + "\n0\n"), "Input exceeds 1024 characters", "Goodbye");
        contains(session(""), "End of input", "Goodbye");
        contains(session("1\nPartial\n"), "End of input", "Goodbye");
        contains(session("1\n/cancel\n10\n0\n"), "Cancelled", "Size: 0");
        contains(session("1\nTitle\nSummary\n/cancel\n10\n0\n"), "Cancelled", "Size: 0");
        contains(session("0\r\n"), "Goodbye");
        contains(session("12\r10\r0\r"), "Loaded 5", "Size: 5", "Goodbye");
        contains(session("12\n5\nDEMO-000001\nno\n10\n0\n"), "Cancelled", "Size: 5");
        String demo = session("12\n6\n7\n4\nDEMO-000003\n8\n9\n10\n11\n0\n");
        contains(demo, "SYNTHETIC", "Project initialization", "Requirements definition",
                "Initial architecture", "First implementation", "Testing and review",
                "Time complexity: O(n)", "Structure diagnostic: O(n)", "O(n^2)",
                "not saved between runs");
        check(demo.indexOf("[DEMO-000001]") < demo.indexOf("[DEMO-000005]"), "demo order");
        String backwards = session("12\n7\n0\n");
        check(backwards.indexOf("DEMO-000005 |") < backwards.indexOf("DEMO-000001 |"),
                "CLI reverse traversal record order");
        contains(session("10\n0\n"), "Size: 0");
        System.out.println("PASS: CLI (" + assertions + " assertions)");
    }

    private static String session(String input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ConsoleMenu(new StringReader(input), output).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void contains(String output, String... fragments) {
        for (String fragment : fragments) {
            check(output.contains(fragment), "missing CLI output: " + fragment + "\n" + output);
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
