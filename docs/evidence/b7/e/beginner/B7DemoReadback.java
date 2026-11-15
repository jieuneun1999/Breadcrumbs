import breadcrumbs.storage.WorkspaceStore;
import java.nio.file.Path;
public class B7DemoReadback {
 public static void main(String[] args) throws Exception {
  var store = new WorkspaceStore(Path.of(args[0]));
  var entries = store.list();
  if (entries.length != 1) throw new AssertionError("Expected one simulated project");
  var state = store.load(entries[0].id());
  var latest = state.checkpoints().getLatest();
  if (state.checkpoints().size() != 1 || state.manualContext().captures().length != 1 || state.laterObservations().length != 1) throw new AssertionError("Demo state count mismatch");
  if (!latest.project().nextActions().equals("Ask the delivery company for a quote")) throw new AssertionError("Next step was not retained");
  if (!state.manualContext().captures()[0].statement().contains("two extra tables")) throw new AssertionError("Reported note was not retained");
  if (state.reviews().length != 1 || !state.reviews()[0].entityId().equals("CHANGE_REVIEW")) throw new AssertionError("Expected one scoped review acknowledgement");
  System.out.println("PASS: Native saved project read back without mutation: 1 update, 1 reported note, 1 later observation, 1 scoped change review; exact next step retained.");
 }
}
