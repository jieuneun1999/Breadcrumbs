package breadcrumbs.inspector;

import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.domain.EvidenceClaim.ArtifactRole;
import breadcrumbs.domain.EvidenceClaim.Field;
import breadcrumbs.domain.EvidenceClaim.Provenance;
import breadcrumbs.domain.EvidenceClaim.ReviewDisposition;
import breadcrumbs.domain.EvidenceClaim.SourceKind;
import breadcrumbs.domain.EvidenceClaim.Verification;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure conversion of bounded bytes into literal, source-located candidates. No filesystem or execution APIs. */
final class ClaimExtractor {
    record Extraction(EvidenceClaim[] claims, SnapshotEntry.Status status, String reason, boolean limited) { }
    private static final Pattern TASK_STATUS = Pattern.compile(
            "^(?:- \\[[ xX]\\] )?((?:TASK|T)-[A-Za-z0-9]+)\\s*[=:]\\s*([A-Za-z_]+)(?:\\s+.*)?$");
    private static final Pattern TASK_ID = Pattern.compile("\\b(?:TASK|T)-[A-Za-z0-9]+\\b");

    private ClaimExtractor() { }

    static ArtifactRole role(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        String name = lower.substring(lower.lastIndexOf('/') + 1);
        if (name.equals("agents.md") || name.equals("claude.md")) {
            return ArtifactRole.INSTRUCTIONS;
        }
        if (name.equals("state.md") || name.equals("status.md") || name.equals("progress.md")) {
            return ArtifactRole.CURRENT_STATE;
        }
        if (name.equals("handoff.md") || name.startsWith("handoff-") && name.endsWith(".md")) {
            return ArtifactRole.HANDOFF;
        }
        if (name.equals("tasks.md") || name.equals("todo.md")) {
            return ArtifactRole.TASK_LIST;
        }
        if (name.equals("decisions.md") || lower.contains("/adr/") && name.endsWith(".md")
                || name.startsWith("adr-") && name.endsWith(".md")) {
            return ArtifactRole.DECISION;
        }
        return ArtifactRole.UNKNOWN;
    }

    static Extraction extract(byte[] bytes, SnapshotEntry.Adapter adapter, String workspaceId,
                              String path, String hash, Instant observedAt, ScanPolicy policy,
                              int firstId, int remainingClaims) {
        try {
            if (adapter == SnapshotEntry.Adapter.DOCX) {
                if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K' || bytes[2] != 3 || bytes[3] != 4) {
                    if (bytes.length >= 4 && bytes[0] == (byte) 0xd0 && bytes[1] == (byte) 0xcf && bytes[2] == 0x11 && bytes[3] == (byte) 0xe0) {
                        return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.FAILED, "Encrypted or legacy compound document cannot be extracted.", false);
                    }
                    return unsupported("DOCX extension has no supported ZIP package signature; no body captured.");
                }
                String[] paragraphs;
                try { paragraphs = DocxExtractor.read(bytes, policy).text(); }
                catch (java.io.IOException | javax.xml.stream.XMLStreamException malformed) {
                    return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.FAILED, "DOCX ZIP/XML invalid, encrypted or exceeds archive safety budgets; no text captured.", false);
                } catch (IllegalArgumentException unsupported) { return unsupported(unsupported.getMessage()); }
                Builder builder = new Builder(workspaceId, path, hash, observedAt, role(path), firstId, remainingClaims);
                for (String paragraph : paragraphs) {
                    if (ContentSafety.hasSecretMarker(java.text.Normalizer.normalize(paragraph, java.text.Normalizer.Form.NFKC))) { return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.SKIPPED, "Sensitive-looking DOCX text not extracted.", false); }
                    for (int i = 0; i < paragraph.length(); i++) {
                        char c = paragraph.charAt(i);
                        if (Character.isISOControl(c) || Character.getType(c) == Character.FORMAT) { return unsupported("Unsafe DOCX text controls; no text captured."); }
                    }
                }
                for (int i = 0; i < paragraphs.length; i++) {
                    String literal = paragraphs[i].strip(); if (literal.isEmpty()) { continue; }
                    int separator = literal.indexOf(':'); Field field = separator > 0 ? field(literal.substring(0, separator)) : null;
                    if (field != null && separator + 1 < literal.length()) { literal = literal.substring(separator + 1).strip(); }
                    else { field = Field.DOCUMENT_TEXT; }
                    builder.add(field, "", literal, literal, i + 1, "DOCX main paragraph " + (i + 1));
                }
                return new Extraction(builder.finish(), SnapshotEntry.Status.SUPPORTED, "Bounded main-document paragraphs only; layout, comments, headers, drawings and embedded content not extracted. Revisions/fields rejected.", builder.limited);
            }
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if (text.startsWith("\ufeff")) {
                text = text.substring(1);
            }
            if (text.length() > policy.maxTextCharacters()) {
                return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.UNSUPPORTED,
                        "Text character limit exceeded.", true);
            }
            int lineLength = 0;
            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                if (character == '\n' || character == '\r') {
                    lineLength = 0;
                } else if (++lineLength > policy.maxLineLength()) {
                    return unsupported("Source line length limit exceeded.");
                }
                if ((Character.isISOControl(character) && character != '\n' && character != '\r'
                        && character != '\t') || Character.getType(character) == Character.FORMAT) {
                    return unsupported("Binary or terminal-control content was not extracted.");
                }
            }
            if (ContentSafety.hasSecretMarker(text)) {
                return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.SKIPPED,
                        "Sensitive-looking source content was not extracted.", false);
            }
            ArtifactRole artifactRole = role(path);
            if (artifactRole == ArtifactRole.INSTRUCTIONS) {
                return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.SUPPORTED,
                        "Instruction artifact fingerprinted; instructions are not progress claims.", false);
            }
            Builder builder = new Builder(workspaceId, path, hash, observedAt, artifactRole,
                    firstId, remainingClaims);
            if (adapter == SnapshotEntry.Adapter.CSV) {
                extractCsv(text, policy, builder);
            } else {
                extractLines(text, adapter, builder);
            }
            return new Extraction(builder.finish(), SnapshotEntry.Status.SUPPORTED,
                    builder.limited ? "Claim limit reached; extraction coverage is partial." : "Bounded extraction completed.",
                    builder.limited);
        } catch (CharacterCodingException malformed) {
            return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.FAILED,
                    "Malformed UTF-8; no claims extracted.", false);
        } catch (IllegalArgumentException malformed) {
            return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.FAILED,
                    malformed.getMessage(), false);
        }
    }

    private static Extraction unsupported(String reason) {
        return new Extraction(new EvidenceClaim[0], SnapshotEntry.Status.UNSUPPORTED, reason, false);
    }

    private static void extractLines(String text, SnapshotEntry.Adapter adapter, Builder builder) {
        String[] lines = text.split("\\r\\n|\\n|\\r", -1);
        String section = "";
        Field sectionField = null;
        boolean inCodeFence = false;
        // A stated source date remains an assertion, never a verified event timestamp.
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.matches("(?i)(?:date|updated at|source date):\\s*\\d{4}-\\d{2}-\\d{2}.*")) {
                builder.sourceDate = trimmed.substring(trimmed.indexOf(':') + 1).strip();
                break;
            }
        }
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].strip();
            if (adapter == SnapshotEntry.Adapter.MARKDOWN && (line.startsWith("```") || line.startsWith("~~~"))) {
                inCodeFence = !inCodeFence;
                continue;
            }
            if (inCodeFence || line.isEmpty()) {
                continue;
            }
            if (adapter == SnapshotEntry.Adapter.MARKDOWN && line.matches("#{1,6}\\s+.*")) {
                section = line.replaceFirst("^#{1,6}\\s+", "");
                sectionField = field(section);
                continue;
            }
            Matcher task = TASK_STATUS.matcher(line);
            if (task.matches()) {
                builder.add(Field.TASK_STATUS, task.group(1), line, task.group(2).toUpperCase(Locale.ROOT),
                        index + 1, section);
                continue;
            }
            if (line.matches("^- \\[[ xX]\\] .+")) {
                Matcher identifier = TASK_ID.matcher(line);
                builder.add(Field.TASK_MARKER, identifier.find() ? identifier.group() : "", line,
                        line.charAt(3) == ' ' ? "UNCHECKED" : "CHECKED", index + 1, section);
                continue;
            }
            int separator = line.indexOf(':');
            Field labeledField = separator > 0 ? field(line.substring(0, separator)) : null;
            if (labeledField != null && separator + 1 < line.length()) {
                builder.add(labeledField, "", line.substring(separator + 1).strip(),
                        line.substring(separator + 1).strip(), index + 1, section);
            } else if (sectionField != null) {
                builder.add(sectionField, "", line, line, index + 1, section);
            }
        }
    }

    private static void extractCsv(String text, ScanPolicy policy, Builder builder) {
        CsvParser.Row[] rows = CsvParser.parse(text, policy);
        if (rows.length == 0) {
            return;
        }
        String[] headers = rows[0].cells();
        int entityColumn = -1;
        for (int column = 0; column < headers.length; column++) {
            for (int other = 0; other < column; other++) {
                if (headers[column].strip().equalsIgnoreCase(headers[other].strip())) {
                    throw new IllegalArgumentException("CSV contains duplicate headers.");
                }
            }
            if (headers[column].strip().equalsIgnoreCase("task_id")) {
                entityColumn = column;
            }
        }
        for (int rowIndex = 1; rowIndex < rows.length; rowIndex++) {
            CsvParser.Row row = rows[rowIndex];
            String entityId = entityColumn < 0 ? "" : row.cells()[entityColumn].strip();
            if (!entityId.matches("(?:TASK|T)-[A-Za-z0-9]+")) {
                entityId = "";
            }
            for (int column = 0; column < headers.length; column++) {
                Field field = field(headers[column].replace('_', ' '));
                if (headers[column].strip().equalsIgnoreCase("status") && !entityId.isEmpty()) {
                    field = Field.TASK_STATUS;
                }
                if (field != null && !row.cells()[column].isBlank()) {
                    builder.add(field, entityId, row.cells()[column], row.cells()[column], row.startLine(),
                            "CSV column " + headers[column]);
                }
            }
        }
    }

    static Field field(String label) {
        return switch (label.strip().toLowerCase(Locale.ROOT)) {
            case "purpose", "objective" -> Field.OBJECTIVE;
            case "deliverable" -> Field.DELIVERABLE;
            case "current state", "status" -> Field.CURRENT_STATE;
            case "next steps", "next action" -> Field.NEXT_ACTION;
            case "owner" -> Field.OWNER;
            case "blockers" -> Field.BLOCKERS;
            case "progress", "completed", "in progress", "pending", "tasks" -> Field.PROGRESS;
            case "decisions", "decision" -> Field.DECISION;
            case "rationale" -> Field.RATIONALE;
            case "test results" -> Field.TEST_RESULTS;
            default -> null;
        };
    }

    private static final class Builder {
        final String workspaceId;
        final String path;
        final String hash;
        final Instant observedAt;
        final ArtifactRole role;
        final int firstId;
        final EvidenceClaim[] claims;
        int size;
        boolean limited;
        String sourceDate = "";

        Builder(String workspaceId, String path, String hash, Instant observedAt, ArtifactRole role,
                int firstId, int capacity) {
            this.workspaceId = workspaceId;
            this.path = path;
            this.hash = hash;
            this.observedAt = observedAt;
            this.role = role;
            this.firstId = firstId;
            claims = new EvidenceClaim[capacity];
        }

        void add(Field field, String entityId, String literal, String comparison, int line, String section) {
            if (size == claims.length) {
                limited = true;
                return;
            }
            claims[size] = new EvidenceClaim(workspaceId,
                    String.format(Locale.ROOT, "EV-%s-%06d", workspaceId, firstId + size), field,
                    entityId, literal, comparison.strip().toUpperCase(Locale.ROOT), path, line, section,
                    hash, role, observedAt, sourceDate, SourceKind.SOURCE_REPORTED, Provenance.EXPLICIT, Verification.UNVERIFIED,
                    ReviewDisposition.PENDING);
            size++;
        }

        EvidenceClaim[] finish() {
            EvidenceClaim[] result = new EvidenceClaim[size];
            System.arraycopy(claims, 0, result, 0, size);
            return result;
        }
    }
}
