package breadcrumbs.inspector;

/** Resource budgets apply even to malformed input. Defaults are deliberately small for classroom use. */
public record ScanPolicy(int maxDepth, int maxEntries, long maxFileBytes, long maxTotalBytes,
                         int maxTextCharacters, int maxLineLength, int maxClaims,
                         int maxCsvColumns, long maxDurationMillis) {
    public ScanPolicy {
        if (maxDepth < 0 || maxDepth > 32 || maxEntries < 1 || maxEntries > 10000
                || maxFileBytes < 1 || maxFileBytes > 1048576 || maxTotalBytes < 1
                || maxTotalBytes > 16777216 || maxTextCharacters < 1 || maxTextCharacters > 262144
                || maxLineLength < 1 || maxLineLength > 4096 || maxClaims < 1 || maxClaims > 2000
                || maxCsvColumns < 1 || maxCsvColumns > 64
                || maxDurationMillis < 1 || maxDurationMillis > 60000) {
            throw new IllegalArgumentException("Scan policy limits are outside supported bounds.");
        }
    }

    public static ScanPolicy defaults() {
        return new ScanPolicy(5, 300, 65536, 2097152, 65536, 1000, 200, 16, 5000);
    }

    public String version() {
        return "B2-1";
    }
}
