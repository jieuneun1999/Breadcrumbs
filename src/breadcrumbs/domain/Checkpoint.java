package breadcrumbs.domain;

import java.time.Instant;

public record Checkpoint(String checkpointId, String title, String summary,
                         Status status, Instant createdAt, Origin origin) {
    public enum Status { PLANNED, IN_PROGRESS, COMPLETED, BLOCKED, UNKNOWN }
    public enum Origin { USER_CONFIRMED, DEMO }

    public Checkpoint {
        checkpointId = text(checkpointId, "Identifier", 45, true);
        title = text(title, "Title", 120, true);
        summary = text(summary, "Summary", 600, false);
        if (status == null || createdAt == null || origin == null) {
            throw new IllegalArgumentException("Status, timestamp, and origin are required.");
        }
        boolean validId = origin == Origin.DEMO
                ? checkpointId.matches("DEMO-[A-Z0-9-]{1,40}")
                : checkpointId.matches("CP-[0-9]{6}") && !checkpointId.equals("CP-000000");
        if (!validId) {
            throw new IllegalArgumentException(origin == Origin.DEMO
                    ? "Synthetic IDs must use DEMO- followed by 1-40 uppercase letters, digits, or hyphens."
                    : "Native IDs must use CP-000001 through CP-999999.");
        }
    }

    private static String text(String value, String name, int limit, boolean required) {
        if (value == null) {
            value = "";
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isISOControl(character) || Character.getType(character) == Character.FORMAT) {
                throw new IllegalArgumentException(name + " must not contain control characters.");
            }
        }
        value = value.strip();
        if (value.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(name + " must not be blank.");
            }
            return "UNKNOWN";
        }
        if (value.length() > limit) {
            throw new IllegalArgumentException(name + " exceeds " + limit + " characters.");
        }
        return value;
    }
}
