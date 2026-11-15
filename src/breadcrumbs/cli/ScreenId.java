package breadcrumbs.cli;

/** Transient locations; project records remain with their existing services. */
public enum ScreenId {
    HOME("Home"), PROJECT_SELECTION("Open project"), PROJECT_SETUP("Project setup"),
    PROJECT_OVERVIEW("Project overview"), PROJECT_WORKSPACE("Project workspace"),
    PROJECT_MAP("Project Map"), PROJECT_MAP_SECTION("Map section"), PROJECT_MAP_DETAIL("Record details"),
    PROGRESS_UPDATE("Progress update"), EVIDENCE_INBOX("Project notes"), SEARCH("Search records"),
    REVIEW_CHANGES("Review file changes"), HANDOFF("Project handoff"), HISTORY("Update history"),
    TASKS("Tasks and dependencies"), ADVANCED_TOOLS("Advanced tools"), RECOVERY("Restore project"),
    ASSESSMENT("Assess project context"), HELP("Help"), RESULT("Project result");
    private final String title;
    ScreenId(String title) { this.title = title; }
    public String title() { return title; }
}
