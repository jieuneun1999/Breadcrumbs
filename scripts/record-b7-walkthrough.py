"""Scripted real Main sessions, stdout/event replay and optional native-window capture.

Requires existing Python/FFmpeg only as developer evidence tools, never to run the app.
No document code or macros are executed. No desktop input injection is used.
"""
from pathlib import Path
import argparse
import ctypes
import codecs
import hashlib
import html
import io
import json
import os
import queue
import re
import shutil
import subprocess
import sys
import threading
import time

REPO = Path(__file__).resolve().parents[1]
SGR = re.compile(r'\x1b\[[0-9;]*m')
TITLE = 'Breadcrumbs B7 public-document walkthrough'
FAST = False


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


class Session:
    def __init__(self, folder, name, color=True, width=160):
        self.folder, self.name, self.events, self.raw, self.screen = folder, name, [], '', ''
        self.queue = queue.Queue()
        env = os.environ.copy()
        env.pop('NO_COLOR', None)
        if not color:
            env['NO_COLOR'] = '1'
        env['COLUMNS'] = str(width)
        self.process = subprocess.Popen(['java', '-cp', str(REPO / 'build/app'), 'breadcrumbs.Main',
                                         '--color=always' if color else '--no-color',
                                         '--storage-root=' + str(folder / 'app')],
                                        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                        encoding='utf-8', text=True, cwd=REPO, env=env)
        threading.Thread(target=self.read, daemon=True).start()

    def read(self):
        decoder = io.IncrementalNewlineDecoder(codecs.getincrementaldecoder('utf-8')(), translate=True)
        while True:
            data = self.process.stdout.buffer.read1(4096)
            text = decoder.decode(data, final=not data)
            if text:
                self.queue.put(text)
            if not data:
                self.queue.put('')
                return

    def until(self, marker):
        incoming = ''
        deadline = time.monotonic() + 40
        while marker not in SGR.sub('', incoming) or not SGR.sub('', incoming).endswith((': ', '> ', '? ')):
            ch = self.queue.get(timeout=max(.01, deadline - time.monotonic()))
            if not ch:
                raise RuntimeError(f'{self.name} exited before {marker}; tail={incoming[-600:]}')
            incoming += ch
        # Publish a whole response at its input boundary: no character/line-by-line typewriter effect.
        sys.stdout.write(incoming)
        sys.stdout.flush()
        self.raw += incoming
        self.screen += incoming
        self.events.append({'kind': 'stdout', 'text': incoming})

    def answer(self, marker, value, pause=0):
        self.until(marker)
        if pause:
            time.sleep(pause)
        elif not FAST:
            time.sleep(.8)
        print('\x1b[0m' + value, flush=True)
        self.screen += value + '\n'
        self.events.append({'kind': 'scripted-input', 'text': value + '\n'})
        self.process.stdin.write(value + '\n')
        self.process.stdin.flush()

    def finish(self):
        self.process.stdin.close()
        self.process.wait(timeout=20)
        tail = ''
        while True:
            ch = self.queue.get(timeout=5)
            if not ch:
                break
            tail += ch
        print(tail, end='', flush=True)
        self.raw += tail
        self.screen += tail
        self.events.append({'kind': 'stdout', 'text': tail})
        (self.folder / (self.name + '-stdout.txt')).write_text(self.raw, encoding='utf-8')
        (self.folder / (self.name + '-events.json')).write_text(json.dumps(self.events, indent=2), encoding='utf-8')
        if self.process.returncode or 'Goodbye.' not in self.raw:
            raise RuntimeError(f'Unclean session {self.name}')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--sample', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--record', action='store_true')
    parser.add_argument('--fast', action='store_true')
    args = parser.parse_args()
    folder = args.output.resolve()
    folder.mkdir(parents=True, exist_ok=False)
    source = folder / 'source'
    source.mkdir()
    doc = args.sample / 'project_management_plan.docx'
    if digest(doc) != '437972d69ad2e86137e62f9551689ec7247a253dacff83b11ad54645d2ea92c0':
        raise RuntimeError('Public DOCX identity differs; review before capture')
    # Preserve the unmodified public original; simulated work notes are clearly separate.
    (source / 'work-notes.md').write_text('# Objective\nReview the public project-plan template.\n# Next Action\nDraft a scope statement.\n', encoding='utf-8')
    shutil.copyfile(doc, source / doc.name)
    shutil.copyfile(args.sample / 'reference.pdf', source / 'reference.pdf')
    before = {p.name: digest(p) for p in source.iterdir()}
    capture = None
    if os.name == 'nt':
        kernel = ctypes.windll.kernel32
        kernel.SetConsoleTitleW(TITLE)
        handle = kernel.GetStdHandle(-11)
        mode = ctypes.c_ulong()
        if kernel.GetConsoleMode(handle, ctypes.byref(mode)):
            kernel.SetConsoleMode(handle, mode.value | 4)
    global FAST
    FAST = args.fast
    pause = 0 if args.fast else 5
    sessions, frames = [], []

    def chapter(title, explanation, session=None):
        print('\n\x1b[1;38;5;214m[WALKTHROUGH] ' + title + '\x1b[0m\n' + explanation + '\n', flush=True)
        if session:
            frames.append({'title': title, 'explanation': explanation, 'text': SGR.sub('', '\n'.join(session.screen.splitlines()[-34:]))})
        if pause:
            time.sleep(pause)

    try:
        if args.record:
            time.sleep(2)
            capture = subprocess.Popen(['ffmpeg', '-hide_banner', '-loglevel', 'warning', '-f', 'gdigrab', '-framerate', '12',
                                        '-i', 'title=' + TITLE, '-an', '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '22',
                                        '-pix_fmt', 'yuv420p', '-vf', 'pad=ceil(iw/2)*2:ceil(ih/2)*2', str(folder / 'walkthrough.mp4')],
                                       stdin=subprocess.PIPE, stdout=subprocess.DEVNULL,
                                       stderr=(folder / 'capture.txt').open('w', encoding='utf-8'))
            time.sleep(2)
            if capture.poll() is not None:
                raise RuntimeError('Native window capture failed; inspect capture.txt')
        chapter('1. Start with an unfamiliar public project document', '90 KB government template, 453 paragraphs. Empty app storage. Inputs are scripted; all application output is real. Piped Java input uses concise nonanimated feedback.')
        s = Session(folder, '01-first-import'); sessions.append(s)
        s.answer('breadcrumbs > ', '2', pause)
        for marker, value in [('Entry mode: ', '2'), ('Project display name: ', 'Public project-plan review'),
                              ('blank = GENERAL]: ', 'OPERATIONS'), ('Existing local project directory: ', str(source)),
                              ('Approve bounded read-only scan? Type yes: ', 'yes'), ('Review [c = confirm', 's')]:
            s.answer(marker, value)
        s.answer('Establish B0 from this reviewed proposal? Type yes: ', 'yes', pause)
        chapter('2. B0 is an observation, not invented prior work', 'The large template has partial claim coverage. TOC fields are omitted; the PDF is fingerprinted only. No CP-000001 exists yet.', s)
        s.answer('Save this workspace to local app storage? Type yes; Enter keeps session-only: ', 'yes')
        s.answer('breadcrumbs > ', '7')
        s.answer('daily > ', '1')
        s.until('daily > ')
        chapter('3. Follow shows what is known and unknown', 'The app cannot know your previous work from a template. Record a present assessment explicitly.', s)
        # Complete the already-observed prompt directly.
        s.process.stdin.write('10\n'); s.process.stdin.flush(); print('10', flush=True)
        s.events.append({'kind': 'scripted-input', 'text': '10\n'})
        for marker, value in [('inbox > ', '1'), ('Capture type: ', '3'), ('Literal statement: ', 'Demo reviewer requested an accessible venue plan'),
                              ('Declared origin: ', 'Simulated planning meeting'), ('Optional declared reference', 'Demo meeting A'),
                              ('Sensitivity [', ''), ('c=record and confirm', 'c'), ('Specifically allow', 'yes'), ('Allow this literal', 'yes'),
                              ('Also record an explicit', 'yes'), ('Leave a Breadcrumb linking', 'yes'),
                              ('What changed or what did you work on? ', 'Reviewed the public template and captured the meeting request'),
                              ('Anything blocked', 'Awaiting stakeholder review'), ('Next concrete action: ', 'Draft the accessible venue scope')]:
            s.answer(marker, value)
        s.answer('yes=confirm,', 'yes', pause)
        chapter('4. Leave a Breadcrumb creates the first native checkpoint', 'Three questions, review, then confirmation. The meeting report remains SOURCE_REPORTED and independently unverified.', s)
        for marker, value in [('daily > ', '11'), ('documents > ', '5'), ('Enable this preference? Type yes: ', 'yes'), ('documents > ', '6'),
                              ('Selected checkpoint IDs', 'CP-000001'), ('New absolute local package directory', str(folder / 'portable')), ('I reviewed privacy/redaction', 'yes')]:
            s.answer(marker, value)
        s.until('documents > ')
        chapter('5. Readable documents and a portable package', 'Canonical history is saved first. state.md, HANDOFF.md and the checkpoint summary are generated separately. Only approved manual bodies are exported.', s)
        s.process.stdin.write('0\n'); s.process.stdin.flush(); print('0', flush=True)
        s.events.append({'kind': 'scripted-input', 'text': '0\n'})
        s.answer('daily > ', '0'); s.answer('breadcrumbs > ', '0'); s.finish()
        chapter('6. Exit and restart the real Java application', 'This launches a new process against the same saved storage. No automatic source scan occurs.')
        s = Session(folder, '02-restart-search'); sessions.append(s)
        for marker, value in [('breadcrumbs > ', '1'), ('Project number (0 = cancel): ', '1'), ('breadcrumbs > ', '7'), ('daily > ', '1')]: s.answer(marker, value)
        s.until('daily > ')
        chapter('7. Resume the checkpoint and find approved context', 'The saved next action and blocker survive restart. Search resolves canonical manual IDs rather than pretending the note came from a file.', s)
        s.process.stdin.write('5\n'); s.process.stdin.flush(); print('5', flush=True); s.events.append({'kind': 'scripted-input', 'text': '5\n'})
        for marker, value in [('search > ', '4'), ('search > ', '1'), ('Exact term: ', 'accessible')]: s.answer(marker, value)
        s.answer('record > ', '0', pause)
        s.answer('search > ', '3')
        s.until('search > ')
        chapter('8. Coverage stays honest', 'The public DOCX was parsed within bounds. The PDF body was never searched. A missing result is not proof that the original contains no information.', s)
        s.process.stdin.write('0\n'); s.process.stdin.flush(); print('0', flush=True); s.events.append({'kind': 'scripted-input', 'text': '0\n'})
        # Only the explicitly synthetic companion note is edited by the harness.
        (source / 'work-notes.md').write_text('# Objective\nReview revised demo scope.\n# Next Action\nAsk the planning reviewer.\n', encoding='utf-8')
        chapter('9. Review an external change with explicit consent', 'The harness changed only its synthetic work-notes.md. The downloaded DOCX/PDF remain untouched.')
        for marker, value in [('daily > ', '4'), ('Rescan source files now?', 'yes'), ('[1] Acknowledge review', '1'), ('Review acknowledgement', 'yes'), ('daily > ', '1')]: s.answer(marker, value)
        s.until('daily > ')
        chapter('10. Review is not a claim of completed work', 'Saved observations and original checkpoint provenance coexist. An acknowledgement does not verify tests or stakeholder approval.', s)
        s.process.stdin.write('0\n'); s.process.stdin.flush(); print('0', flush=True); s.events.append({'kind': 'scripted-input', 'text': '0\n'})
        s.answer('breadcrumbs > ', '0'); s.finish()
        chapter('11. Standalone handoff', 'The package can be read without Breadcrumbs. Full-format acceptance and actual Java 17 runtime verification remain separate pending gates.')
        for filename in ['state.md', 'checkpoints/CP-000001.md']:
            print('\x1b[1;38;5;222m' + filename + '\x1b[0m', flush=True)
            print('\n'.join((folder / 'portable' / filename).read_text(encoding='utf-8').splitlines()[:24]), flush=True)
            time.sleep(pause)
        assert digest(source / doc.name) == before[doc.name]
        assert digest(source / 'reference.pdf') == before['reference.pdf']
        manifest = {'public_docx': before[doc.name], 'public_pdf': before['reference.pdf'], 'source_files': before,
                    'app_class_sha256': digest(REPO / 'build/app/breadcrumbs/Main.class'), 'capture': 'native window screen recording' if args.record else 'not recorded',
                    'input': 'scripted through real Java stdin; concise feedback fallback', 'frames': frames}
        (folder / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
        cards = ''.join('<section><h2>' + html.escape(f['title']) + '</h2><p>' + html.escape(f['explanation']) + '</p><pre>' + html.escape(f['text']) + '</pre></section>' for f in frames)
        (folder / 'walkthrough.html').write_text('<!doctype html><meta charset="utf-8"><title>Breadcrumbs real terminal walkthrough</title><style>body{background:#151412;color:#eee;font:18px system-ui;margin:40px}h2{color:#ffcf80}section{margin:48px 0}pre{background:#20201e;padding:20px;overflow:auto;font:15px Consolas,monospace;white-space:pre-wrap}p{max-width:850px}</style><h1>Breadcrumbs: public-document walkthrough</h1><p>Real Java output with scripted input. This HTML is a readable chapter transcript; the MP4 is a separate native screen recording.</p>' + cards, encoding='utf-8')
    finally:
        for session in sessions:
            if session.process.poll() is None:
                session.process.kill()
                session.process.wait(timeout=5)
        if capture and capture.poll() is None:
            capture.stdin.write(b'q\n'); capture.stdin.flush(); capture.wait(timeout=20)
    print('\nWALKTHROUGH COMPLETE: ' + str(folder), flush=True)
    time.sleep(pause)


if __name__ == '__main__':
    main()
