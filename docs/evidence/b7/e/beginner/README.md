# Beginner presentation evidence

The frames are actual native terminal captures of a simulated project. No real
community work or private document is represented. The raw MP4 and generated
project/storage/export folders stay local; recording-identity.json identifies them.
Text evidence redacts local repository paths and trims trailing whitespace. Native
frames are unaltered. Original logs remain in the local gate/demo folders.

The final native gate covers all source, tests, scripts and the Windows launcher.
Its full result and relative input hashes are retained here. Presentation-copy
migrations are listed in docs/B7_EXECPLAN.md; state/effect assertions remain.

To reproduce the populated Main preflight, build the app with scripts/build.bat,
then run `python scripts/record-b7-beginner.py --output build/new-demo-name`.
The output directory must not exist. Native recording additionally requires the
compiled NativeWalkthrough harness, a real 160-column Windows console and the
existing FFmpeg executable. The script never installs these tools.

The saved-state readback helper runs with a full JDK:
`java --source 17 --class-path build/app docs/evidence/b7/e/beginner/B7DemoReadback.java <demo-native-app-folder>`.
The observed readback also compared every saved file's byte hash before and after.
This is Java17-target source execution on JDK25, not proof of an actual JDK17 runtime.
