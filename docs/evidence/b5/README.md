# B5 actual execution evidence

All records are synthetic classroom fixtures. No claim that fictional task actions
occurred is made. Tests use disposable app storage and never execute source projects.

- tests-junction.txt records5540 passing suite assertions, including1090 B5.
- process.txt records10 assertions across separate production Main processes.
- b4-process.txt records the unchanged55 B4 A–F process assertions.
- graph-cli.txt/restart.txt show explicit task creation, dependency chain, BFS/DFS,
  potential impact, rejected cycle and saved/reopened Graph.
- a-chain-cycle.txt and b-remove-edge.txt show real separate-process mutations.
- c-historical-handoff.txt shows original checkpoint dependency revision after a
  later edge removal, plus Hash collision/resize and indexed/linear parity.
- hash-cli.txt shows synthetic insert/update/lookup/remove/buckets and actual index
  comparison, toggle, rebuild/validation. No demo data becomes canonical records.
- source-sha256.txt binds96 source/test/script/fixture/run.bat files to exercised bytes.

Logs normalize text line endings for Git review. Raw local outputs remain under
build/b5-final, build/b5-process and build/b5-fixtures. The genuine pre-B5 v2 fixture
is tests/fixtures/b4-v2/workspace.txt; v1 fixtures remain unchanged. B5A's original
gate is independently checkpointed at5b44fbb before Hash code existed.

Observed environment Windows10/JDK25, Java17 target. Native ACL/junction checks pass.
Actual JDK17 runtime, Unix/instructor hardware and symbolic-link creation remain
unverified. The shell wrapper initially failed in this host environment, then recovered.
The documented scripts\test-b5.bat passed the full suite and both process runners.
CLI text is actual app output, not a mockup/screenshot or native-terminal visual proof.
