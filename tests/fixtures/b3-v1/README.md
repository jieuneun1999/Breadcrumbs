# Genuine B3 version-1 compatibility fixtures

These files were produced by the actual B3 implementation before B4 codec changes.
They contain synthetic classroom/test data, not user project history. Their saved
absolute source roots are inert historical locators; loading must never inspect them.

| File | Provenance | SHA-256 |
| --- | --- | --- |
| workspace-1.txt | B3 accepted launch walkthrough, synthetic Thesis Research, protected before B4 codec edits | 9ef4e0f38872565d4406a35f3d3800a58cdf8a64bfdc3b9b7af51994fd78273f |
| imported-workspace.txt | B3 ContinuityTest's W003 source-linked checkpoint and conflict fixture; original file modified 2026-10-30T12:45:44.656192Z | c41f85d5d8f56a39720303a73567298d31f9c0447be933d9427862c54f774581 |

Tests verify exact-byte v1 read/write, read-only open, explicit v2 save, restored
original B0/CP/evidence references, high-water sequence and unchanged provenance.
Do not regenerate these fixtures with the new writer; that would weaken migration proof.
