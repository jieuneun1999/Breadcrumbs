package breadcrumbs.b6;

import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.domain.*;
import breadcrumbs.search.*;
import breadcrumbs.continuity.ObservationService;
import breadcrumbs.storage.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;
import static breadcrumbs.b4.Checks.*;

public final class SearchTest {
    private SearchTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b6-fixtures")), "index-").toAbsolutePath();
        Path source = Files.createDirectory(base.resolve("source"));
        Files.writeString(source.resolve("notes.md"), "# Objective\nmethod methodology methodology methods\nIgnored? no, captured section\n");
        Files.writeString(source.resolve("plan.txt"), "Next action: methodology\nuncapturedword\n");
        Files.writeString(source.resolve("table.csv"), "objective,owner\nmethodology,privateowner\n");
        Files.writeString(source.resolve("AGENTS.md"), "# Objective\ninstructiononly\n");
        Files.writeString(source.resolve(".env"), "secret=credentialfixture\n");
        Files.writeString(source.resolve("sensitive.txt"), "Objective: secret=forbiddenword\n");
        Files.writeString(source.resolve("escape.md"), "# Objective\n\u001b[31munsafe\n");
        Files.write(source.resolve("malformed.txt"), new byte[] {(byte) 0xc3, (byte) 0x28});
        for (String extension : new String[] {"pdf", "docx", "xlsx", "pptx", "java", "json", "xml", "yml", "png"}) {
            Files.writeString(source.resolve("unsupported." + extension), "bodyonlyword");
        }
        WorkspaceState state = WorkflowTest.imported(source, "W601");
        ContextSearchIndex index = new ContextSearchIndex(state);
        SearchPolicy policy = SearchPolicy.defaults();
        require(index.searchExact("methodology", 20).status() == SearchReport.Status.INDEX_UNAVAILABLE, "unbuilt unavailable");
        require(index.rebuild(policy, () -> false), "complete default rebuild");
        require(index.searchExact("methodology", 20).hits().length == 0, "unreviewed fields excluded by default");
        policy = policy.withUnreviewed(true); index.setPolicy(policy);
        require(index.searchExact("methodology", 20).status() == SearchReport.Status.INDEX_OUTDATED, "policy invalidation");
        require(index.rebuild(policy, () -> false), "explicit captured-source policy");
        var found = index.searchExact("methodology", 20);
        require(found.hits().length == 3 && found.totalMatches() == 3, "one exact term multiple evidence fields");
        require(found.hits()[0].occurrences() == 2 || found.hits()[1].occurrences() == 2 || found.hits()[2].occurrences() == 2, "one posting counts repetition");
        for (var hit : found.hits()) {
            require(hit.location().workspaceId().equals("W601") && !hit.location().historical(), "current scoped evidence");
            require(hit.location().truth().contains("SOURCE_REPORTED") && hit.location().locator().contains("line"), "source truth and reliable locator");
            require(index.viewField(hit.location(), false).isEmpty(), "no default snippet");
            require(index.viewField(hit.location(), true).isEmpty(), "pending source snippet withheld");
        }
        for (String excluded : new String[] {"uncapturedword", "instructiononly", "forbiddenword", "credentialfixture", "bodyonlyword", "privateowner", "unsafe"}) {
            require(index.searchExact(excluded, 20).totalMatches() == 0, "excluded or unextracted term " + excluded);
        }
        require(Arrays.equals(index.suggestTerms("meth", 10).terms(), new String[] {"method", "methodology", "methods"}), "Trie dictionary agrees with exact terms");
        require(index.validateAgainstCanonicalState(), "canonical dictionary validation");
        require(index.coverage().contains("failed/unstable=1") && index.coverage().contains("sensitivity-skipped="), "failed extraction and sensitivity coverage");
        require(index.searchExact("methodology", 1).limited() && index.searchExact("methodology", 1).totalMatches() == 3, "result cap reported");
        var first = WorkflowTest.find(state, "notes.md", EvidenceClaim.Field.OBJECTIVE);
        state.addReview(new ContextReview(state.currentObservationId(), first.evidenceId(), first.field(), first.entityId(),
                first.literalValue(), EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
        require(!index.isCurrent(state) && index.searchExact("methodology", 20).hits().length == 0, "review change never returns stale postings");
        require(index.rebuild(SearchPolicy.defaults(), () -> false), "confirmed evidence default index");
        var confirmed = index.searchExact("methodology", 20).hits();
        require(confirmed.length == 1 && index.viewField(confirmed[0].location(), true).contains("methodology"), "approved bounded field view");
        WorkflowTest.checkpoint(state, "Methodology checkpoint", new String[] {first.evidenceId()});
        require(index.status() == SearchReport.Status.INDEX_OUTDATED, "checkpoint invalidation");
        index.rebuild(policy, () -> false);
        require(index.searchExact("methodology", 20).hits().length == 4, "evidence and native checkpoint match");
        require(index.searchExact("methodology", 20).hits()[3].location().historical(), "checkpoint explicitly historical");
        state.dependencies().create("Methodology work", "methodology methodology", WorkItem.Status.UNKNOWN, "", null, new String[0], true);
        require(index.status() == SearchReport.Status.INDEX_OUTDATED, "work-item invalidation");
        index.rebuild(policy, () -> false);
        require(index.searchExact("methodology", 20).hits().length == 6, "current canonical work-item fields");
        byte[] original = WorkspaceCodec.encode(state);
        require(!index.rebuild(policy, () -> true) && index.isCurrent(state), "cancelled rebuild keeps valid previous index");
        Files.writeString(source.resolve("notes.md"), "# Objective\nreplacementword\n");
        Files.delete(source.resolve("plan.txt"));
        new ObservationService().scan(state, true, n -> true);
        require(index.searchExact("methodology", 20).status() == SearchReport.Status.INDEX_OUTDATED, "rescan invalidates");
        require(!index.rebuild(policy, () -> true) && index.searchExact("methodology", 20).hits().length == 0, "cancelled stale candidate not exposed");
        require(index.rebuild(policy, () -> false), "new observation rebuild");
        var after = index.searchExact("methodology", 20).hits();
        require(after.length == 4 && index.searchExact("replacementword", 20).hits().length == 1, "removed source terms disappear, history and work remain labeled");
        for (var hit : after) { require(!hit.location().recordId().equals(first.evidenceId()), "no historical-source masquerade"); }
        require(index.coverage().contains("metadata-only") && index.coverage().contains("captured fields only") && index.coverage().contains("policy-excluded"), "honest coverage");
        var tiny = new SearchPolicy(1, 64, 1000000, 2000, 50000, 2000, 10, 5000, 20, 160, 10000, 200000, true, "TINY", "NFKC-ROOT-1", "CAPTURED-1");
        require(!index.rebuild(tiny, () -> false) && index.searchExact("methodology", 20).hits().length == 0, "capacity cannot publish partial results");
        index.rebuild(policy, () -> false); index.setEnabled(false);
        require(index.searchExact("methodology", 20).status() == SearchReport.Status.DISABLED, "disabled index explicit rebuild alternative");
        index.setEnabled(true); require(index.isCurrent(state), "reenable current projection");
        var store = new WorkspaceStore(base.resolve("app")); store.save(state);
        WorkspaceState restored = store.load("W601"); ContextSearchIndex restoredIndex = new ContextSearchIndex(restored);
        require(restoredIndex.status() == SearchReport.Status.INDEX_UNAVAILABLE && restoredIndex.rebuild(policy, () -> false), "restore reconstructs disposable index");
        require(Arrays.equals(WorkspaceCodec.encode(restored), WorkspaceCodec.encode(state)), "search does not mutate canonical storage");
        WorkspaceState other = WorkflowTest.imported(source, "W602"); state.replaceWith(other);
        require(index.status() == SearchReport.Status.INDEX_OUTDATED && !index.isCurrent(restored), "workspace isolation and object identity");
        index.rebuild(policy, () -> false);
        for (var hit : index.searchExact("replacementword", 20).hits()) { require(hit.location().workspaceId().equals("W602"), "no foreign hits"); }
        WorkspaceState old = WorkspaceCodec.decode(original); ContextSearchIndex oldIndex = new ContextSearchIndex(old);
        oldIndex.rebuild(policy, () -> false); old.checkpoints().removeById("CP-000001");
        require(oldIndex.status() == SearchReport.Status.INDEX_OUTDATED, "direct DLL deletion invalidates");
        for (String fixture : new String[] {"tests/fixtures/b3-v1/imported-workspace.txt", "tests/fixtures/b4-v2/workspace.txt"}) {
            byte[] bytes = Files.readAllBytes(Path.of(fixture)); var loaded = WorkspaceCodec.decode(bytes);
            ContextSearchIndex legacy = new ContextSearchIndex(loaded); require(legacy.rebuild(policy, () -> false), "old-format canonical search build");
            require(Arrays.equals(bytes, WorkspaceCodec.encode(loaded)), "search never upgrades old storage");
        }
        System.out.println("PASS: B6 canonical search, safety, revisions, coverage and restore | " + base);
    }
}
