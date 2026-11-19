# Classroom requirements checklist

Use `scripts/DSA_DEMO.bat` for the professor-facing session. `ClassroomConsoleTest` is
included in `AllTests`. Results and screenshot locations are in the
[local delivery report](CLASSROOM_FINALIZATION_REPORT.md).

| Requirement | Code | Test or observation | In class | Status |
| --- | --- | --- | --- | --- |
| Primary custom DLL, manual node with both links | `CheckpointNode`, `CheckpointList` | `CheckpointListTest` | Show source and **9** | PASS |
| At least six operations | `append`, `prepend`, `insertAt`, `findById`, `removeById`, `traverseForward`, `traverseBackward`, `getLatest`, `size` | DLL and classroom tests | **1–8**, **10** | PASS |
| Five starting records | `ConsoleMenu.run` calls `DemoData.load` before first menu | Classroom test and start capture | Launch and show count | PASS |
| At least three useful fields | `Checkpoint.checkpointId/title/summary`, `ConsoleMenu.show` | Classroom traversal and successful search | **4**, enter `DEMO-000003` | PASS |
| Numbered menu and return after each action | `ConsoleMenu.menu/run` | Classroom test exercises sequential actions | Enter **9**, then **10** | PASS |
| Visible links and endpoints | `CheckpointList.displayStructure`, `ConsoleMenu.report` | DLL test and actual console captures | **9** | PASS |
| Validation and safe edge cases | `ConsoleMenu.create/delete/read`, `CheckpointList.validateIntegrity` | Duplicate and missing ID, bad position and empty/single-node tests | **4**, enter a missing ID. **3**, enter `99`. | PASS |
| Per-operation complexity | `ConsoleMenu.report` and DLL operation JavaDocs | Classroom checks assert traversal space and integrity time | Compare **8**, **9**, **11** | PASS |
| Comments above operations | JavaDocs on `CheckpointList`, `TaskDependencyGraph`, `CustomHashTable`, `PrefixTrie` | Source review and strict build | Open DSA sources | PASS |
| Clean compile | `scripts/build.bat` | `--release 17 -Xlint:all -Werror` | Launch or run the build | PASS |
| Windows one-click launch | `run.bat` and `scripts/DSA_DEMO.bat` | See launcher evidence and machine-specific limits in the delivery report | Open in Explorer | NOT VERIFIED |
| Actual group name and six real screenshots | `Main.main`, `ConsoleMenu.menu` | Four normal and two error captures, view source-bound evidence | Show menu title and screenshots | PASS |
| Actual Java 17 runtime | JDK 17 minimum, Java 17 class target | Local Windows runtime was JDK 25 | Check classroom `java --version` | NOT VERIFIED |
| Report, individual contributions and named submission archive | [Mae's documentation notes](MAE_DOCUMENTATION_HANDOFF.md) | Formal report and packaging happen after owner supplies details | Prepare for live questions | NEEDS OWNER INPUT |
