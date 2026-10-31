"""Native production-ProjectConsole capture with scripted Reader and verified console VT.

Two separate Java processes exercise restart. This avoids pipe-driven character
rendering and permits the real capability-gated two-part brand intro. Developer
tools and capture pacing are separate from the JDK-only application.
"""
from pathlib import Path
import argparse
import ctypes
import hashlib
import json
import os
import shutil
import subprocess
import time

REPO = Path(__file__).resolve().parents[1]
TITLE = 'Breadcrumbs calm public-document walkthrough'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--sample', type=Path, required=True)
    parser.add_argument('--tapes', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    folder = args.output.resolve()
    folder.mkdir(parents=True, exist_ok=False)
    source = folder / 'source'
    source.mkdir()
    expected = '437972d69ad2e86137e62f9551689ec7247a253dacff83b11ad54645d2ea92c0'
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    if digest(args.sample / 'project_management_plan.docx') != expected:
        raise RuntimeError('Public sample identity changed; inspect before use')
    for name in ['project_management_plan.docx', 'reference.pdf']:
        shutil.copyfile(args.sample / name, source / name)
    (source / 'work-notes.md').write_text('# Objective\nReview the public project-plan template.\n# Next Action\nDraft a scope statement.\n', encoding='utf-8')
    kernel = ctypes.windll.kernel32
    kernel.SetConsoleTitleW(TITLE)
    handle, mode = kernel.GetStdHandle(-11), ctypes.c_ulong()
    if not kernel.GetConsoleMode(handle, ctypes.byref(mode)) or not kernel.SetConsoleMode(handle, mode.value | 4):
        raise RuntimeError('This native console does not support process-local VT')
    env = os.environ.copy()
    env['BREADCRUMBS_NATIVE_VT'] = '1'
    log = (folder / 'capture.txt').open('w', encoding='utf-8')
    capture = subprocess.Popen(['ffmpeg', '-hide_banner', '-loglevel', 'warning', '-f', 'gdigrab', '-framerate', '24', '-i', 'title=' + TITLE,
                                '-an', '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '22', '-pix_fmt', 'yuv420p',
                                '-vf', 'pad=ceil(iw/2)*2:ceil(ih/2)*2', str(folder / 'walkthrough.mp4')],
                               stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=log)
    try:
        time.sleep(2)
        if capture.poll() is not None:
            raise RuntimeError('Window capture failed')
        print('REAL BREADCRUMBS CONSOLE | Public DOCX + labeled simulated notes\nScripted input at reading pace. No imported code runs. No prior session.\n', flush=True)
        for number, name in enumerate(['01-first-import', '02-restart-search'], 1):
            if number == 2:
                print('\nRESTART | First Java process exited. Opening the same saved storage in a new process.\n', flush=True)
                # The selected tape includes explicit source review; modify only the simulated note.
                (source / 'work-notes.md').write_text('# Objective\nReview revised demo scope.\n# Next Action\nAsk the planning reviewer.\n', encoding='utf-8')
                time.sleep(4)
            events = json.loads((args.tapes / (name + '-events.json')).read_text(encoding='utf-8'))
            answers = ''.join(e['text'] for e in events if e['kind'] == 'scripted-input').replace(str(args.tapes.resolve()), str(folder))
            input_file = folder / (name + '.input.txt')
            input_file.write_text(answers, encoding='utf-8')
            command = ['java', '-cp', str(REPO / 'build/app') + os.pathsep + str(REPO / 'build/native-demo'),
                       'breadcrumbs.cli.NativeWalkthrough', str(input_file), str(folder / 'app'), str(folder / (name + '-native.txt'))]
            result = subprocess.run(command, cwd=REPO, env=env, timeout=160)
            if result.returncode:
                raise RuntimeError('Native console session failed')
            text = (folder / (name + '-native.txt')).read_text(encoding='utf-8')
            if 'Goodbye.' not in text:
                raise RuntimeError('No clean exit')
        assert digest(source / 'project_management_plan.docx') == expected
        assert (folder / 'portable/state.md').exists()
        print('\nDONE | Checkpoint saved, reopened, searched and reviewed.\nPortable state.md / HANDOFF.md / checkpoint summary are available outside the app.\nPublic document unchanged. Full representative-format acceptance remains pending.\n', flush=True)
        time.sleep(6)
        files = {str(p.relative_to(REPO)): digest(p) for p in (REPO / 'build/app').rglob('*.class')}
        manifest = {'capture': 'native production ProjectConsole; two separate Java processes; scripted Reader',
                    'vt': 'real console mode read/set succeeded before Java inherited the same screen buffer',
                    'intro': 'real production two-part 120ms brand path; no character/line pacing',
                    'pacing': 'Reader only: 1s per input, 2.2s confirmations, 6s first input; no product-operation delay',
                    'public_docx_sha256': expected, 'class_hashes': files}
        (folder / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    finally:
        if capture.poll() is None:
            capture.stdin.write(b'q\n'); capture.stdin.flush(); capture.wait(timeout=20)
        log.close()


if __name__ == '__main__':
    main()
