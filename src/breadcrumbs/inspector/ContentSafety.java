package breadcrumbs.inspector;

import java.util.Locale;

/** Shared policy for untrusted filenames, source snippets, and terminal rendering. */
public final class ContentSafety {
    private ContentSafety() { }

    public static boolean excludedName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String excluded : new String[] {".git", "node_modules", "build", "target", "dist",
                ".venv", "venv", ".cache", ".gradle", ".m2", ".npm", ".idea", ".breadcrumbs",
                "__pycache__", ".ssh", ".aws", ".azure", ".npmrc", ".pypirc", ".netrc"}) {
            if (lower.equals(excluded)) {
                return true;
            }
        }
        return lower.startsWith(".env") || lower.contains("credential") || lower.contains("secret")
                || lower.contains("password") || lower.contains("token") || lower.startsWith("id_rsa")
                || lower.startsWith("id_ed25519") || lower.endsWith(".pem") || lower.endsWith(".key")
                || lower.endsWith(".pfx") || lower.endsWith(".p12") || lower.endsWith(".jks")
                || lower.endsWith(".kdbx") || lower.endsWith(".keystore") || lower.endsWith(".tmp")
                || lower.endsWith(".temp") || lower.endsWith(".swp") || lower.endsWith("~");
    }

    public static boolean hasSecretMarker(String text) {
        return text.matches("(?is).*(?:api[_ -]?key|access[_ -]?token|password|secret|bearer|private[_ -]?key)\\s*[:=].*")
                || text.contains("-----BEGIN ") && text.contains("PRIVATE KEY-----");
    }

    /** Replace controls rather than emitting ANSI, carriage-return, bidi, or invisible formatting commands. */
    public static String terminal(String value, int limit) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < value.length() && output.length() < limit; i++) {
            char character = value.charAt(i);
            output.append(Character.isISOControl(character)
                    || Character.getType(character) == Character.FORMAT ? '?' : character);
        }
        if (value.length() > limit) {
            output.append("...");
        }
        return output.toString();
    }
}
