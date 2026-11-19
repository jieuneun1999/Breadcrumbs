# Codebase guide

Application source is under [src/breadcrumbs](../src/breadcrumbs/). The Windows
launchers resolve their own location, build the Java entry point and pass options
to `Main`. The classroom and production launchers share these sources.

| Area | Responsibility and call path | Verification |
| --- | --- | --- |
| [Main and CLI](../src/breadcrumbs/cli/) | `Main.main` selects the mode. Classroom mode uses `ConsoleMenu` and `DemoData`. `ProjectConsole`, `BootstrapConsole` and `DailyConsole` handle the full workflow. `TerminalScreenManager`, `TerminalKeyboard` and presentation classes handle rendering/input. | [ClassroomConsoleTest](../tests/breadcrumbs/cli/ClassroomConsoleTest.java), [CLI tests](../tests/breadcrumbs/cli/) |
| [Domain](../src/breadcrumbs/domain/) and [checkpoints](../src/breadcrumbs/checkpoint/) | `Checkpoint` holds each timeline record. `WorkspaceState` holds confirmed data and references. `ProjectCheckpointService` and `QuickCheckpointService` connect checkpoint capture to continuity. | [CheckpointListTest](../tests/breadcrumbs/dsa/dll/CheckpointListTest.java), [B7 tests](../tests/breadcrumbs/b7/) |
| [DLL](../src/breadcrumbs/dsa/dll/) | `CheckpointList` manages `CheckpointNode` instances containing a record and both neighbor pointers. It owns endpoints, size, access, updates and integrity checks. The production workspace uses the same DLL. | [CheckpointListTest](../tests/breadcrumbs/dsa/dll/CheckpointListTest.java), [ClassroomConsoleTest](../tests/breadcrumbs/cli/ClassroomConsoleTest.java) |
| [Graph](../src/breadcrumbs/dsa/graph/) | `TaskDependencyGraph` stores explicitly declared prerequisite/dependent edges in custom adjacency lists. It rejects cycles and supports BFS/DFS through dependency links. | [B5 tests](../tests/breadcrumbs/b5/) |
| [Hash](../src/breadcrumbs/dsa/hash/) and [indexes](../src/breadcrumbs/index/) | `CustomHashTable` uses an array of buckets and linked collision chains. `WorkspaceIndexes` projects lookup keys from workspace records and can be rebuilt. | [B5 tests](../tests/breadcrumbs/b5/) |
| [Trie](../src/breadcrumbs/dsa/trie/) and [search](../src/breadcrumbs/search/) | `ContextSearchIndex` builds policy-eligible fields into a custom trie for prefix suggestions and hash-backed posting chains for exact terms. Hits resolve to canonical field IDs and locators. | [B6 tests](../tests/breadcrumbs/b6/), [B7 tests](../tests/breadcrumbs/b7/) |
| [Inspector](../src/breadcrumbs/inspector/) | `ProjectInspector` validates the local root, enumerates with limits and fingerprints eligible files. `ClaimExtractor` routes configured text/CSV fields and `DocxExtractor` handles bounded paragraphs. Bootstrap builds a B0 observation for review. | [Inspector tests](../tests/breadcrumbs/inspector/), [B7 tests](../tests/breadcrumbs/b7/) |
| [Storage](../src/breadcrumbs/storage/) | `WorkspaceStore` and `WorkspaceCodec` handle supported state formats. `GenerationStore` validates recovery manifests and pointers. `ArtifactService` builds derived Markdown and portable views. | [ContinuityTest](../tests/breadcrumbs/storage/ContinuityTest.java), [recovery process test](../tests/breadcrumbs/storage/B7RecoveryProcessTest.java) |
| [Continuity and handoff](../src/breadcrumbs/continuity/) | `ProjectMapService`, `ReferenceFreshness` and `DriftEngine` resolve present context and changes. `HandoffService` and `HandoffExporter` prepare exportable views and write new outputs. | [B4 tests](../tests/breadcrumbs/b4/), [Project Map process test](../tests/breadcrumbs/b7/ProjectMapProcessTest.java) |

For the DLL defense, start with `CheckpointNode`, then read
`CheckpointList.append`, `prepend`, `insertAt`, `removeById` and
`validateIntegrity`. Follow `ConsoleMenu` for input, complexity reporting and
the displayed links. [Classroom operation table](B7_CLASSROOM_DELIVERY.md)
