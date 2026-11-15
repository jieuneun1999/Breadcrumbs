package breadcrumbs.dsa.dll;

import breadcrumbs.domain.Checkpoint;

final class CheckpointNode {
    final Checkpoint checkpoint;
    CheckpointNode previous;
    CheckpointNode next;

    CheckpointNode(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }
}
