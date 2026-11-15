package breadcrumbs.inspector;

import breadcrumbs.domain.*;
import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.search.*;
import breadcrumbs.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.*;
import static breadcrumbs.b4.Checks.*;

public final class DocxTest {
    private DocxTest() { }
    private static final String NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    public static byte[] document(String body) throws IOException {
        return archive(new String[] {"[Content_Types].xml", "word/document.xml"}, new String[] {"<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>", "<w:document xmlns:w=\"" + NS + "\"><w:body>" + body + "</w:body></w:document>"});
    }
    private static byte[] archive(String[] names, String[] bodies) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (int i = 0; i < names.length; i++) { zip.putNextEntry(new ZipEntry(names[i])); zip.write(bodies[i].getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
        }
        return bytes.toByteArray();
    }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "docx-").toAbsolutePath();
        Path source = Files.createDirectory(base.resolve("source"));
        byte[] good = document("<w:p/><w:p><w:r><w:t>Objective: Research </w:t></w:r><w:r><w:t>accessibility</w:t></w:r></w:p><w:p><w:r><w:t>ordinaryparagraphword</w:t></w:r></w:p>");
        Files.write(source.resolve("research.docx"), good);
        var scan = new ProjectInspector().inspect(source, "W731", ScanPolicy.defaults(), n -> true);
        require(scan.entries()[0].status() == SnapshotEntry.Status.SUPPORTED && scan.claims().length == 2, "DOCX main paragraphs actually extracted");
        require(scan.claims()[0].line() == 2 && scan.claims()[0].section().equals("DOCX main paragraph 2") && scan.claims()[0].literalValue().equals("Research accessibility"), "split runs and blank paragraphs retain true paragraph ordinals");
        require(scan.claims()[1].literalValue().equals("ordinaryparagraphword"), "unlabeled main-document paragraph captured as literal text");
        var state = WorkflowTest.imported(source, "W731");
        for (var c : state.activeBaseline().claims()) { state.addReview(new ContextReview(state.currentObservationId(), c.evidenceId(), c.field(), c.entityId(), c.literalValue(), EvidenceClaim.ReviewDisposition.CONFIRMED, java.time.Instant.now())); }
        var search = new ContextSearchIndex(state); search.rebuild(SearchPolicy.defaults(), () -> false);
        var hits = search.searchExact("ordinaryparagraphword", 10).hits();
        require(hits.length == 1 && hits[0].location().locator().contains("paragraph 3") && !hits[0].location().locator().contains("line 3"), "DOCX search uses truthful paragraphs");
        var store = new WorkspaceStore(base.resolve("app")); store.save(state); var restart = store.load("W731");
        require(restart.activeBaseline().claims()[0].section().equals("DOCX main paragraph 2"), "DOCX locator survives canonical restart");
        require(java.util.Arrays.equals(good, Files.readAllBytes(source.resolve("research.docx"))), "document original remains byte-identical");
        Files.write(source.resolve("fields.docx"), document("<w:p><w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText>fieldinstructionword</w:instrText></w:r><w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t>cachedfieldword</w:t></w:r><w:r><w:fldChar w:fldCharType=\"end\"/></w:r></w:p><w:p><w:r><w:t>ordinarybodyword</w:t></w:r></w:p>"));
        var fields = new ProjectInspector().inspect(source, "W731", ScanPolicy.defaults(), n -> true);
        int fieldClaims = 0; for (var c : fields.claims()) { if (c.relativePath().equals("fields.docx")) { require(c.line() == 2 && c.literalValue().equals("ordinarybodyword"), "field instructions and cached results omitted; ordinary paragraph locator retained"); fieldClaims++; } }
        require(fieldClaims == 1, "normal body remains useful in documents containing TOC/cross-reference fields");
        bad(source, document("<w:p><w:r><w:t>password=synthetic</w:t></w:r></w:p>"), SnapshotEntry.Status.SKIPPED, "sensitive text");
        bad(source, document("<w:p><w:r><w:t>" + "x".repeat(1100) + "</w:t></w:r></w:p>"), SnapshotEntry.Status.UNSUPPORTED, "paragraph bound");
        bad(source, document("<w:p><w:ins><w:r><w:t>ambiguous revision</w:t></w:r></w:ins></w:p>"), SnapshotEntry.Status.UNSUPPORTED, "tracked changes not silently flattened");
        bad(source, archive(new String[] {"../escape", "word/document.xml"}, new String[] {"escape", "<bad/>"}), SnapshotEntry.Status.FAILED, "ZIP traversal");
        bad(source, archive(new String[] {"[Content_Types].xml", "word/document.xml"}, new String[] {"types", "<!DOCTYPE x [<!ENTITY xxe SYSTEM 'file:///unapproved'>]><x>&xxe;</x>"}), SnapshotEntry.Status.FAILED, "DTD/entities");
        bad(source, archive(new String[] {"[Content_Types].xml", "word/document.xml"}, new String[] {"types", "<notclosed>"}), SnapshotEntry.Status.FAILED, "malformed XML");
        bad(source, archive(new String[] {"bomb"}, new String[] {"z".repeat(300000)}), SnapshotEntry.Status.FAILED, "inflated ZIP size/ratio");
        String[] names = new String[129], bodies = new String[129]; for (int i = 0; i < names.length; i++) { names[i] = "part" + i; bodies[i] = "part"; }
        bad(source, archive(names, bodies), SnapshotEntry.Status.FAILED, "ZIP entry limit");
        bad(source, new byte[] {(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0}, SnapshotEntry.Status.FAILED, "encrypted/legacy compound document");
        bad(source, "plain text with a DOCX suffix".getBytes(StandardCharsets.UTF_8), SnapshotEntry.Status.UNSUPPORTED, "mismatched extension/package signature");
        byte[] encrypted = good.clone(); encrypted[6] |= 1; bad(source, encrypted, SnapshotEntry.Status.FAILED, "encrypted ZIP entry");
        System.out.println("PASS: B7D DOCX paragraph extraction, search/restart and ZIP/XML safety | " + base);
    }
    private static void bad(Path source, byte[] bytes, SnapshotEntry.Status expected, String label) throws Exception {
        Files.write(source.resolve("negative.docx"), bytes);
        var scan = new ProjectInspector().inspect(source, "W731", ScanPolicy.defaults(), n -> true);
        SnapshotEntry entry = null; for (var e : scan.entries()) { if (e.relativePath().equals("negative.docx")) { entry = e; } }
        require(entry != null && entry.status() == expected && !entry.sha256().isEmpty(), label + " rejected with an honest fingerprint/status");
        for (var c : scan.claims()) { require(!c.relativePath().equals("negative.docx"), label + " emits no partial claims"); }
    }
}
