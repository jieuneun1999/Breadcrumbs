package breadcrumbs.demo;

import breadcrumbs.domain.Checkpoint;
import breadcrumbs.domain.Checkpoint.Origin;
import breadcrumbs.domain.Checkpoint.Status;
import breadcrumbs.dsa.dll.CheckpointList;
import java.time.Instant;

public final class DemoData {
    private DemoData() { }

    public static int load(CheckpointList list) {
        String[] titles = {"Project initialization", "Requirements definition", "Initial architecture",
                "First implementation", "Testing and review"};
        String[] summaries = {"SYNTHETIC: Created a study planner outline. Next: define requirements.",
                "SYNTHETIC: Listed planner use cases. Next: sketch component responsibilities.",
                "SYNTHETIC: Sketched planner modules. Next: implement a small feature.",
                "SYNTHETIC: Drafted planner entry flow. Next: run fixture tests.",
                "SYNTHETIC: Reviewed planner fixtures. Next: request human feedback."};
        int added = 0;
        for (int i = 0; i < titles.length; i++) {
            String id = "DEMO-00000" + (i + 1);
            if (list.findById(id) == null) {
                list.append(new Checkpoint(id, titles[i], summaries[i], Status.COMPLETED,
                        Instant.parse("2026-01-01T09:00:00Z").plusSeconds(i * 86400L), Origin.DEMO));
                added++;
            }
        }
        return added;
    }
}
