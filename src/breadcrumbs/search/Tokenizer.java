package breadcrumbs.search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

public final class Tokenizer {
    private Tokenizer() { }
    public static String[] tokens(String text, SearchPolicy policy) {
        if (text == null || text.length() > policy.maxCharacters()) { throw new IllegalArgumentException("Indexed character limit exceeded."); }
        checkUnicode(text);
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if (normalized.length() > policy.maxCharacters()) { throw new IllegalArgumentException("Normalized character limit exceeded."); }
        String[] tokens = new String[policy.maxTokensPerField()]; int count = 0, length = 0;
        StringBuilder term = new StringBuilder();
        for (int offset = 0; offset <= normalized.length();) {
            int point = offset == normalized.length() ? -1 : normalized.codePointAt(offset);
            boolean lexical = point >= 0 && (Character.isLetterOrDigit(point) || length > 0 && mark(point));
            if (lexical) {
                if (++length > policy.maxTermLength()) { throw new IllegalArgumentException("Term length limit exceeded."); }
                term.appendCodePoint(point);
            } else if (length > 0) {
                if (count == tokens.length) { throw new IllegalArgumentException("Per-field token limit exceeded."); }
                tokens[count++] = term.toString(); term.setLength(0); length = 0;
            }
            if (point < 0) { break; }
            offset += Character.charCount(point);
        }
        return Arrays.copyOf(tokens, count);
    }
    public static String query(String text, SearchPolicy policy) {
        if (text == null || text.length() > policy.maxTermLength() * 4) { throw new IllegalArgumentException("Query too long."); }
        String[] terms = tokens(text, policy);
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if (terms.length != 1 || !terms[0].equals(normalized)) { throw new IllegalArgumentException("Enter one nonempty lexical term or prefix."); }
        return terms[0];
    }
    public static void checkUnicode(String text) {
        for (int i = 0; i < text.length();) {
            char c = text.charAt(i); int point = text.codePointAt(i);
            if (Character.isSurrogate(c) && Character.charCount(point) == 1
                    || Character.isISOControl(point) && point != '\n' && point != '\r' && point != '\t'
                    || Character.getType(point) == Character.FORMAT) {
                throw new IllegalArgumentException("Unsafe or malformed Unicode text excluded.");
            }
            i += Character.charCount(point);
        }
    }
    private static boolean mark(int point) {
        int type = Character.getType(point);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK;
    }
}
