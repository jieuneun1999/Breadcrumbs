package breadcrumbs.dsa.graph;

/** Internal vertex identity; canonical status/owner stays in WorkspaceState. */
final class GraphVertex {
    final String id;
    EdgeNode firstOutgoing;
    GraphVertex(String id) { this.id = id; }
}
