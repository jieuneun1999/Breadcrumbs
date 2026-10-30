package breadcrumbs.inspector;

/** Small bounded CSV state machine; it does not infer meaning from arbitrary columns. */
final class CsvParser {
    record Row(String[] cells, int startLine) { }

    private CsvParser() { }

    static Row[] parse(String text, ScanPolicy policy) {
        Row[] rows = new Row[policy.maxClaims() + 1];
        String[] cells = new String[policy.maxCsvColumns()];
        StringBuilder cell = new StringBuilder();
        int rowCount = 0;
        int columnCount = 0;
        int line = 1;
        int rowStart = 1;
        boolean quoted = false;
        boolean quoteClosed = false;
        boolean rowStarted = false;
        for (int index = 0; index <= text.length(); index++) {
            boolean end = index == text.length();
            char character = end ? '\n' : text.charAt(index);
            if (character == '\r') {
                if (index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
                character = '\n';
            }
            if (quoted) {
                if (end) {
                    throw new IllegalArgumentException("CSV has an unclosed quoted field.");
                }
                if (character == '"') {
                    if (index + 1 < text.length() && text.charAt(index + 1) == '"') {
                        cell.append('"');
                        index++;
                    } else {
                        quoted = false;
                        quoteClosed = true;
                    }
                } else {
                    cell.append(character);
                    if (character == '\n') {
                        line++;
                    }
                }
            } else if (character == ',' || character == '\n') {
                if (end && !rowStarted && cell.length() == 0 && columnCount == 0) {
                    break;
                }
                if (columnCount == cells.length) {
                    throw new IllegalArgumentException("CSV column limit exceeded.");
                }
                cells[columnCount++] = cell.toString();
                cell.setLength(0);
                quoteClosed = false;
                rowStarted = character == ',';
                if (character == '\n') {
                    if (rowCount == rows.length) {
                        throw new IllegalArgumentException("CSV row limit exceeded.");
                    }
                    if (rowCount > 0 && columnCount != rows[0].cells().length) {
                        throw new IllegalArgumentException("CSV rows have inconsistent column counts.");
                    }
                    String[] row = new String[columnCount];
                    System.arraycopy(cells, 0, row, 0, columnCount);
                    rows[rowCount++] = new Row(row, rowStart);
                    columnCount = 0;
                    line++;
                    rowStart = line;
                }
            } else if (character == '"' && cell.length() == 0 && !quoteClosed) {
                quoted = true;
                rowStarted = true;
            } else {
                if (quoteClosed || character == '"') {
                    throw new IllegalArgumentException("CSV contains an unexpected quote or trailing field text.");
                }
                cell.append(character);
                rowStarted = true;
            }
            if (cell.length() > policy.maxLineLength()) {
                throw new IllegalArgumentException("CSV field length limit exceeded.");
            }
        }
        Row[] result = new Row[rowCount];
        System.arraycopy(rows, 0, result, 0, rowCount);
        return result;
    }
}
