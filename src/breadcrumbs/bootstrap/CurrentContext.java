package breadcrumbs.bootstrap;

import breadcrumbs.inspector.ContentSafety;

/** Human-supplied present context. Blank information stays explicitly unknown. */
public record CurrentContext(String objective, String deliverable, String nextAction,
                             String owner, String blockers) {
    public CurrentContext {
        objective = normalize(objective);
        deliverable = normalize(deliverable);
        nextAction = normalize(nextAction);
        owner = normalize(owner);
        blockers = normalize(blockers);
    }

    public static CurrentContext unknown() {
        return new CurrentContext("", "", "", "", "");
    }

    private static String normalize(String value) {
        String normalized = value == null || value.isBlank() ? "UNKNOWN" : value.strip();
        if (normalized.length() > 600 || !ContentSafety.terminal(normalized, 600).equals(normalized)
                || ContentSafety.hasSecretMarker(normalized)) {
            throw new IllegalArgumentException("Use at most 600 safe, non-sensitive characters per context field.");
        }
        return normalized;
    }
}
