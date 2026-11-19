# Breadcrumbs notes for Mae

Use the official project title **Breadcrumbs: An Evidence-Driven Project Continuity
and Context Management System** and the supplied group name **The Bread**. The
product line is “Leave context behind. Pick up where work stopped.” The classroom
menu adds “We are the bread and we ain't letting no crumbs”.

## Try the app before writing the report

- Open **DEMO.bat** with JDK 17 or newer. Follow the
  [first-project walkthrough](../README.md#your-first-project) to create a
  practice project, save an update, close Breadcrumbs, and reopen its summary.
- Open **DSA_DEMO.bat** and choose **9 Display structure**. The five seeded
  checkpoints make the head, tail and links visible immediately. See the
  [ten-minute classroom plan](B7_CLASSROOM_DELIVERY.md#ten-minute-classroom-plan)
  for what to present and which error cases to show.

## Report references

| Report material | Source or evidence |
| --- | --- |
| Problem and user workflow | [Project README](../README.md) describes creation, B0 review, recording work and resuming local projects. |
| Primary structure and record | [CheckpointNode](../src/breadcrumbs/dsa/dll/CheckpointNode.java), [CheckpointList](../src/breadcrumbs/dsa/dll/CheckpointList.java), [Checkpoint](../src/breadcrumbs/domain/Checkpoint.java). Use ID, title and summary in a record example. |
| Structure illustration | Recreate the relationships from option **9**. HEAD has a null previous link, TAIL has a null next link, and adjacent nodes point to each other. Use the IDs in the actual capture. |
| Algorithms and costs | [Classroom delivery](B7_CLASSROOM_DELIVERY.md) and JavaDocs. Show the ID scan and the display/verification costs separately from relinking. Graph, Hash and Trie support the application. |
| Normal and error screenshots | [Local delivery report](CLASSROOM_FINALIZATION_REPORT.md) identifies the four normal and two error captures and their verification status. The menu must show The Bread in each selected frame. |
| Scope, fixtures and citations | [Codebase guide](CODEBASE_GUIDE.md), [classroom requirements](CLASSROOM_REQUIREMENTS_CHECKLIST.md) and [earlier B7 evidence](B7_FINAL_ACCEPTANCE.md). Cite sources and AI assistance actually used. Confirm attribution with the team. |

The class guideline says the report PDF should be **5–8 pages**. The supplied
template spans **nine pages**. Confirm how the instructor wants that resolved
before laying out the report. The template is a format reference for headings
and sequence. Use Breadcrumbs' own results and screenshots for examples.

The team will supply the section, group number, member names and IDs, and actual
individual contributions. The archive naming format is
`Section_GroupNo_ProjectTitle`. Include the reviewed source under `src`, the
README and the finished report PDF. Keep saved workspaces, private input
documents and local builds out of the submission materials.
