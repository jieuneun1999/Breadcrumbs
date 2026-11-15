package breadcrumbs.dsa.graph;

import breadcrumbs.domain.Dependency;

/** Integer slots resolve adjacent vertices without a repeated linear ID search during traversal. */
final class EdgeNode {
    final int target;
    final Dependency declaration;
    EdgeNode next;
    EdgeNode(int target, Dependency declaration, EdgeNode next) {
        this.target = target; this.declaration = declaration; this.next = next;
    }
}
