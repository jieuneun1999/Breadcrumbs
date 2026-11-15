package breadcrumbs.b4;

import java.io.IOException;

/** Tiny dependency-free assertions shared by the B4 behavioral suites. */
public final class Checks {
    private static int count;
    private Checks() { }
    public static void require(boolean value, String message) {
        count++; if (!value) { throw new AssertionError(message); }
    }
    public static void close(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < .000001, message + ": " + actual + " != " + expected);
    }
    @FunctionalInterface public interface Action { void run() throws Exception; }
    public static void reject(Action action, String message) throws Exception {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException | IOException expected) { rejected = true; }
        require(rejected, message);
    }
    public static int count() { return count; }
}
