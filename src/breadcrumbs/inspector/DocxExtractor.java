package breadcrumbs.inspector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.stream.*;

/** In-memory, bounded main-part reader. No filesystem handles, relationship resolution or execution. */
final class DocxExtractor {
    private static final String WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    record Paragraphs(String[] text) { }
    private DocxExtractor() { }
    static Paragraphs read(byte[] bytes, ScanPolicy policy) throws IOException, XMLStreamException {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K' || bytes[2] != 3 || bytes[3] != 4) { throw new IOException("Not an unencrypted DOCX ZIP package."); }
        byte[] main = null; boolean contentTypes = false; String[] names = new String[128]; int count = 0, total = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (count == names.length) { throw new IOException("DOCX entry limit exceeded."); }
                String name = entry.getName();
                if (name.length() > 512 || name.startsWith("/") || name.contains("\\") || name.contains(":") || name.matches(".*(?:^|/)\\.\\.?(?:/|$).*")) { throw new IOException("Unsafe DOCX member path."); }
                for (int i = 0; i < count; i++) { if (names[i].equals(name)) { throw new IOException("Duplicate DOCX member."); } }
                names[count++] = name;
                var part = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int length;
                while ((length = zip.read(buffer)) != -1) {
                    total += length;
                    if (part.size() + length > 262144 || total > 1048576) { throw new IOException("DOCX inflated byte budget exceeded."); }
                    part.write(buffer, 0, length);
                }
                zip.closeEntry();
                if (entry.getCompressedSize() <= 0 && part.size() > 0 || part.size() > Math.max(1, entry.getCompressedSize()) * 100) { throw new IOException("DOCX compression ratio limit exceeded."); }
                if (name.equals("word/document.xml")) { main = part.toByteArray(); }
                contentTypes |= name.equals("[Content_Types].xml");
            }
        }
        if (main == null || !contentTypes) { throw new IOException("DOCX main document/content-types part missing."); }
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXMLResolver((publicId, systemId, base, namespace) -> { throw new XMLStreamException("External resources forbidden."); });
        var reader = factory.createXMLStreamReader(new ByteArrayInputStream(main));
        String[] paragraphs = new String[2000]; int used = 0, depth = 0, characters = 0, events = 0; boolean root = false, body = false, inParagraph = false, inText = false;
        StringBuilder paragraph = new StringBuilder();
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (++events > 100000) { throw new IOException("DOCX XML event limit exceeded."); }
                if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) { throw new XMLStreamException("DTD and entities forbidden."); }
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (++depth > 64) { throw new IOException("DOCX XML nesting limit exceeded."); }
                    if (!root) { if (!reader.getLocalName().equals("document") || !WORD.equals(reader.getNamespaceURI())) { throw new XMLStreamException("Unsupported Word document namespace/root."); } root = true; }
                    if (!WORD.equals(reader.getNamespaceURI())) { continue; }
                    String name = reader.getLocalName();
                    if (name.equals("ins") || name.equals("del") || name.equals("moveFrom") || name.equals("moveTo") || name.equals("altChunk") || name.equals("instrText") || name.equals("fldSimple")) { throw new IllegalArgumentException("Tracked changes, alternate content and field instructions are not supported."); }
                    if (name.equals("body")) { body = true; }
                    if (body && name.equals("p")) { if (inParagraph || used == paragraphs.length) { throw new IOException("DOCX paragraph structure/count limit exceeded."); } inParagraph = true; paragraph.setLength(0); }
                    if (inParagraph && name.equals("t")) { inText = true; }
                    if (inParagraph && (name.equals("tab") || name.equals("br") || name.equals("cr"))) { paragraph.append(' '); }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (inText) { paragraph.append(reader.getText()); }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (WORD.equals(reader.getNamespaceURI())) {
                        if (reader.getLocalName().equals("t")) { inText = false; }
                        if (reader.getLocalName().equals("p") && inParagraph) { paragraphs[used++] = paragraph.toString(); inParagraph = false; }
                        if (reader.getLocalName().equals("body")) { body = false; }
                    }
                    depth--;
                }
                if (paragraph.length() > policy.maxLineLength()) { throw new IllegalArgumentException("DOCX paragraph limit exceeded."); }
                if (inText && (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA)) { characters += reader.getTextLength(); }
                if (characters > policy.maxTextCharacters()) { throw new IllegalArgumentException("DOCX text character limit exceeded."); }
            }
        } finally { reader.close(); }
        if (!root || inParagraph || depth != 0) { throw new XMLStreamException("Incomplete DOCX XML."); }
        return new Paragraphs(java.util.Arrays.copyOf(paragraphs, used));
    }
}
