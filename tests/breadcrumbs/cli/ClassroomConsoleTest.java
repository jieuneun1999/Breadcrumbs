package breadcrumbs.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public final class ClassroomConsoleTest {
    private ClassroomConsoleTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        String startup = session("6\n7\n8\n9\n10\n11\n0\n");
        check(startup.indexOf("Five SYNTHETIC") < startup.indexOf("[1]"), "Five records exist before the first menu");
        check(startup.contains("BREADCRUMBS | The Bread | DOUBLY LINKED LIST"), "Actual group title");
        check(startup.contains("We are the bread and we ain't letting no crumbs"), "Owner group tagline");
        check(!startup.contains("[13]") && !startup.contains("[14]"), "Focused menu retains only DLL choices");
        check(startup.contains("Size: 5") && startup.contains("HEAD=DEMO-000001 | TAIL=DEMO-000005"), "True endpoints and initial count");
        check(startup.contains("DEMO-000001 | Project initialization") && startup.contains("SYNTHETIC"), "Visible meaningful record fields");
        check(startup.contains("Auxiliary space: O(n) returned array") && startup.contains("Time complexity: O(n^2)"), "Actual operation costs");
        String edits = session("1\nTail record\nSummary\nUNKNOWN\nyes\n2\nDEMO-HEAD\nHead record\nSummary\nPLANNED\nyes\n3\n1\nDEMO-MIDDLE\nMiddle record\nSummary\nBLOCKED\nyes\n4\nDEMO-MIDDLE\n5\nDEMO-HEAD\nyes\n5\nDEMO-NEW-000001\nyes\n10\n0\n");
        check(edits.contains("Found DEMO-MIDDLE") && edits.contains("Deleted DEMO-HEAD") && edits.contains("Deleted DEMO-NEW-000001"), "Tail/head/position/search/delete");
        check(!edits.contains("USER_CONFIRMED") && edits.contains("Size: 6"), "Classroom records always remain synthetic");
        String invalid = session("oops\n99\n3\nno\n3\n-1\n3\n99\n4\nDEMO-MISSING\n5\nDEMO-MISSING\n2\nDEMO-NEW-000001\n2\nDEMO-000001\nDuplicate\nSummary\nUNKNOWN\nyes\n13\n10\n0\n");
        check(invalid.contains("DEMO-NEW- is reserved") && invalid.contains("Position must") && invalid.contains("Duplicate checkpoint ID") && invalid.contains("Not found") && invalid.contains("Size: 5"), "Rejected input preserves the list");
        StringBuilder empty = new StringBuilder();
        for (int i = 1; i <= 5; i++) { empty.append("5\nDEMO-00000").append(i).append("\nyes\n"); }
        empty.append("6\n7\n8\n9\n10\n11\n0\n");
        String result = session(empty.toString());
        check(result.contains("Size: 0") && result.contains("HEAD=NULL | TAIL=NULL") && result.contains("No latest checkpoint"), "Single-node endpoints and empty list");
        try { ConsoleMenu.classroom(new StringReader("0\n"), System.out, new TerminalTheme(false), 120, "bad\u001b[31m"); throw new AssertionError("Unsafe group accepted"); }
        catch (IllegalArgumentException expected) { check(expected.getMessage().contains("printable"), "Group title rejects controls"); }
        System.out.println("PASS: classroom startup, group, synthetic isolation, all DLL routes and input/endpoint errors");
    }
    private static String session(String input) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            ConsoleMenu.classroom(new StringReader(input), out, new TerminalTheme(false), 120, "The Bread").run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
