"""Capture real Java interactions as a local HTML replay, with optional PNG previews.

This developer evidence tool is separate from the JDK-only application. Input
echoes are included for readability; the result is not a desktop screen recording.
"""
from base64 import b64decode
from datetime import datetime
import html
import hashlib
import json
from pathlib import Path
import queue
import re
import subprocess
import threading
import time
from uuid import uuid4

REPOSITORY = Path(__file__).resolve().parents[1]
OUTPUT = REPOSITORY / 'build' / 'launch-walkthrough' / uuid4().hex
STORAGE = OUTPUT / 'synthetic-storage'
SGR = re.compile(r'(\x1b\[[0-9;]*m)')
PALETTE = {
    '1;38;5;214': ('#ffaf00', None, True),
    '1;38;5;222': ('#ffdf87', None, True),
    '38;5;130': ('#af5f00', None, False),
    '1;38;5;222;48;5;235': ('#ffdf87', '#262626', True),
    '38;5;245': ('#8a8a8a', None, False),
    '32': ('#13a10e', None, False),
    '33': ('#c19c00', None, False),
    '31': ('#c50f1f', None, False),
}


def plain(text):
    return SGR.sub('', text)


def runs(text):
    color, background, bold = '#cccccc', None, False
    for part in SGR.split(text):
        if part.startswith('\x1b['):
            code = part[2:-1]
            if code == '0':
                color, background, bold = '#cccccc', None, False
            elif code in PALETTE:
                color, background, bold = PALETTE[code]
            elif code.startswith('1;38;2;'):
                values = [int(value) for value in code.split(';')[1:]]
                if len(values) != 5:
                    raise ValueError(f'Unexpected art-cell code {code}')
                color = '#%02x%02x%02x' % tuple(values[2:5])
                background = None
                bold = True
            else:
                raise ValueError(f'Unexpected terminal code {code}')
        elif part:
            yield part, color, background, bold


class Session:
    def __init__(self, name, columns=160, color=True):
        import os
        environment = os.environ.copy()
        environment.pop('NO_COLOR', None)
        environment['COLUMNS'] = str(columns)
        self.process = subprocess.Popen(
            ['java', '-cp', 'build/app', 'breadcrumbs.Main',
             f'--storage-root={STORAGE}', '--rich-brand', '--color=always' if color else '--no-color'],
            cwd=REPOSITORY, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, text=True, encoding='utf-8', bufsize=1, env=environment,
        )
        self.name, self.color = name, color
        self.events, self.display, self.raw = [], '', ''
        self.incoming = queue.Queue()
        self.reader = threading.Thread(target=self._read, daemon=True)
        self.reader.start()

    def _read(self):
        while True:
            character = self.process.stdout.read(1)
            if not character:
                self.incoming.put(None)
                return
            self.incoming.put(character)

    def until(self, prompt):
        marker = prompt + ('\x1b[0m' if self.color else '')
        received = ''
        deadline = time.monotonic() + 15
        while not received.endswith(marker):
            try:
                character = self.incoming.get(timeout=max(0.01, deadline - time.monotonic()))
            except queue.Empty as error:
                raise RuntimeError(f'{self.name}: prompt deadline exceeded: {prompt}') from error
            if character is None:
                raise RuntimeError(f'{self.name}: exited before prompt {prompt}')
            received += character
            if time.monotonic() >= deadline:
                raise RuntimeError(f'{self.name}: prompt deadline exceeded: {prompt}')
        self.raw += received
        self.display += received
        self.events.append({'kind': 'stdout', 'text': received})

    def send(self, value):
        self.process.stdin.write(value + '\n')
        self.process.stdin.flush()
        self.display += value + '\n'
        self.events.append({'kind': 'input', 'text': value + '\n'})

    def finish(self):
        self.process.stdin.close()
        self.process.wait(timeout=15)
        self.reader.join(timeout=2)
        remaining = ''
        while not self.incoming.empty():
            character = self.incoming.get_nowait()
            if character is not None:
                remaining += character
        self.raw += remaining
        self.display += remaining
        if self.process.returncode != 0:
            raise RuntimeError(f'{self.name}: Java exit {self.process.returncode}')
        (OUTPUT / f'{self.name}-stdout.txt').write_text(self.raw, encoding='utf-8')
        (OUTPUT / f'{self.name}-events.json').write_text(json.dumps(self.events, indent=2), encoding='utf-8')

    def stop(self):
        if self.process.poll() is None:
            self.process.kill()
            self.process.wait(timeout=5)


frames = []


def snapshot(session, title, explanation):
    # Viewport cropping models terminal scrolling; complete stdout is retained separately.
    lines = session.display.lstrip('\n').split('\n')
    show_startup = title in ('Launch', 'Restart', 'Plain-text fallback', 'Narrow terminal')
    text = '\n'.join(lines if show_startup else lines[-32:])
    frames.append({'title': title, 'explanation': explanation, 'ansi': text})


def drive():
    first = Session('create-and-checkpoint')
    try:
        first.until('breadcrumbs > ')
        snapshot(first, 'Launch', 'First run. Empty synthetic storage, real initialization and numbered actions.')
        for prompt, value in [
            ('Entry mode: ', '1'), ('Project display name: ', 'Thesis Research'),
            ('Profile [GENERAL, ACADEMIC, OPERATIONS, SOFTWARE; blank = GENERAL]: ', 'ACADEMIC'),
            ('Objective (blank = UNKNOWN): ', 'Write a clear research proposal'),
            ('Intended deliverable (blank = UNKNOWN): ', 'Research proposal'),
            ('Current next action (blank = UNKNOWN): ', 'Review the outline'),
            ('Optional owner (blank = UNKNOWN): ', 'Student'),
            ('Optional blockers (blank = UNKNOWN; enter none only if stated): ', ''),
            ('Optional existing EMPTY directory (blank = no directory observation): ', ''),
            ('Establish B0 from this reviewed proposal? Type yes: ', 'yes'),
            ('Save this workspace to local app storage? Type yes; Enter keeps session-only: ', 'yes'),
        ]:
            if prompt == 'Entry mode: ':
                first.send('2')
            first.until(prompt)
            first.send(value)
        first.until('breadcrumbs > ')
        snapshot(first, 'B0 saved', 'A real NEW_EMPTY baseline is confirmed and saved. No checkpoint is inferred.')
        first.send('3')
        first.until('project > ')
        first.send('2')
        for prompt, value in [
            ('Checkpoint title: ', 'Outline reviewed'), ('Progress summary: ', 'Initial outline recorded'),
            ('Objective: ', 'Write a clear research proposal'),
            ('Blockers [UNKNOWN, NONE_REPORTED, BLOCKED]: ', 'UNKNOWN'), ('Blocker details: ', ''),
            ('Next actions: ', 'Review related literature'), ('Decisions / rationale: ', 'Keep the first outline short'),
            ('Owner: ', 'Student'), ('Evidence IDs separated by commas (blank = none): ', ''),
            ('Create this native checkpoint? Type yes: ', 'yes'),
        ]:
            first.until(prompt)
            first.send(value)
        first.until('project > ')
        assert 'Confirmed CP-000001' in plain(first.display)
        snapshot(first, 'Checkpoint confirmed', 'Explicit confirmation appends CP-000001 to the custom DLL and saves it.')
        first.send('0')
        first.until('breadcrumbs > ')
        first.send('0')
        first.finish()
    finally:
        first.stop()

    second = Session('restart-and-resume')
    try:
        second.until('breadcrumbs > ')
        assert 'Saved projects: 1' in plain(second.display)
        snapshot(second, 'Restart', 'A fresh Java process discovers one saved workspace. Its source folder is not scanned.')
        second.send('1')
        second.until('Project number (0 = cancel): ')
        second.send('1')
        second.until('breadcrumbs > ')
        second.send('3')
        second.until('project > ')
        second.send('3')
        second.until('project > ')
        assert 'Outline reviewed' in plain(second.display)
        snapshot(second, 'Resume checkpoint', 'The original objective, blockers and next actions return from saved state.')
        second.send('8')
        second.until('project > ')
        assert 'Integrity: true' in plain(second.display)
        snapshot(second, 'DLL integrity', 'The restored manual list contains CP-000001 with valid links.')
        second.send('0')
        second.until('breadcrumbs > ')
        second.send('0')
        second.finish()
    finally:
        second.stop()
    for name, columns in [('Plain-text fallback', 80), ('Narrow terminal', 30)]:
        session = Session(name.lower().replace(' ', '-'), columns=columns, color=False)
        try:
            session.until('breadcrumbs > ')
            snapshot(session, name, f'Actual no-color output at {columns} columns. No special font or Unicode required.')
            session.send('0')
            session.finish()
        finally:
            session.stop()
    state_file, = STORAGE.glob('workspaces/*/state.txt')
    fields = state_file.read_text(encoding='utf-8').splitlines()
    assert 'nextCheckpoint=Mg==' in fields
    assert 'id=Q1AtMDAwMDAx' in fields
    assert not any('DEMO-' in b64decode(line.split('=', 1)[1]).decode('utf-8')
                   for line in fields[1:-1])


def save_html():
    for frame in frames:
        markup = []
        for text, color, background, bold in runs(frame['ansi']):
            style = f'color:{color};font-weight:{700 if bold else 400};'
            if background:
                style += f'background:{background};'
            markup.append(f'<span style="{style}">{html.escape(text)}</span>')
        frame['html'] = ''.join(markup)
    data = json.dumps(frames).replace('</', '<\\/')
    document = '''<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Breadcrumbs | captured CLI walkthrough</title>
<style>
body{margin:0;background:#171717;color:#ddd;font:15px system-ui,sans-serif}
main{max-width:1280px;margin:28px auto;padding:0 22px}h1{font-size:22px;margin:0 0 8px}
p{line-height:1.5;color:#bdbdbd}nav{display:flex;gap:8px;flex-wrap:wrap;margin:20px 0}
button{font:inherit;color:#ddd;background:#282828;border:1px solid #555;padding:8px 12px;cursor:pointer}
button[aria-pressed=true]{border-color:#ffaf00;color:#ffdf87}button:focus-visible{outline:2px solid #ffdf87;outline-offset:3px}
pre{background:#0c0c0c;margin:0;padding:24px;overflow:auto;max-height:76vh;box-sizing:border-box;
font:13px/1.25 Consolas,'Cascadia Mono',monospace;font-variant-ligatures:none;tab-size:4}
footer{display:flex;align-items:center;justify-content:space-between;gap:12px;margin:14px 0}
#description{min-height:44px}#position{color:#ffdf87}a{color:#ffdf87}
</style><main><h1>Breadcrumbs terminal walkthrough</h1>
<p>Captured from the running Java CLI with synthetic local storage. Input echoes are included for readability.
This is an interactive replay, not a desktop screen recording or an application GUI.</p>
<nav aria-label="Captured steps" id="steps"></nav><p id="description" aria-live="polite"></p>
<pre id="terminal" tabindex="0" aria-label="Captured terminal output"></pre>
<footer><button id="previous">Previous</button><span id="position"></span><button id="next">Next</button></footer>
<p>Use the step buttons or Left/Right arrow keys. Complete output and input-event logs are saved beside this file.</p>
</main><script>
const frames=__FRAMES__;let current=0;
const nav=document.getElementById('steps'),terminal=document.getElementById('terminal');
frames.forEach((frame,i)=>{const button=document.createElement('button');button.textContent=frame.title;
button.onclick=()=>show(i);nav.append(button)});
function show(i){current=Math.max(0,Math.min(frames.length-1,i));terminal.innerHTML=frames[current].html;
document.getElementById('description').textContent=frames[current].explanation;
document.getElementById('position').textContent=`${current+1} / ${frames.length}`;
[...nav.children].forEach((button,index)=>button.setAttribute('aria-pressed',String(index===current)));
document.getElementById('previous').disabled=current===0;document.getElementById('next').disabled=current===frames.length-1;
terminal.scrollTop=(current===0||current===3||current>=6)?0:terminal.scrollHeight;}
document.getElementById('previous').onclick=()=>show(current-1);
document.getElementById('next').onclick=()=>show(current+1);
document.addEventListener('keydown',event=>{if(event.key==='ArrowRight'){show(current+1);event.preventDefault()}
if(event.key==='ArrowLeft'){show(current-1);event.preventDefault()}});show(0);
</script></html>'''.replace('__FRAMES__', data)
    (OUTPUT / 'walkthrough.html').write_text(document, encoding='utf-8')


def save_png():
    try:
        from PIL import Image, ImageDraw, ImageFont
    except ImportError:
        print('PNG skipped: optional Pillow is unavailable. HTML replay is complete.')
        return
    try:
        font = ImageFont.truetype('C:/Windows/Fonts/consola.ttf', 14)
        bold_font = ImageFont.truetype('C:/Windows/Fonts/consolab.ttf', 14)
    except OSError:
        print('PNG skipped: optional Consolas preview font is unavailable. HTML replay is complete.')
        return
    cell, row_height, margin = font.getlength('M'), 18, 24
    for frame in (frames[0], frames[-1]):
        columns = max(len(line) for line in plain(frame['ansi']).split('\n')) + 1
        height = (plain(frame['ansi']).count('\n') + 1) * row_height + margin * 2
        image = Image.new('RGB', (int(columns * cell + margin * 2), height), '#0c0c0c')
        draw = ImageDraw.Draw(image)
        x, y = margin, margin
        for text, color, background, bold in runs(frame['ansi']):
            for character in text:
                if character == '\n':
                    x, y = margin, y + row_height
                    continue
                if background:
                    draw.rectangle((x, y, x + cell, y + row_height), fill=background)
                draw.text((x, y), character, fill=color, font=bold_font if bold else font)
                x += cell
        draw.rectangle((x, y + 2, x + cell - 1, y + 21), fill='#ccc')
        image.save(OUTPUT / ('launch.png' if frame is frames[0] else 'narrow.png'))


def main():
    OUTPUT.mkdir(parents=True)
    inputs = sorted((REPOSITORY / 'src').rglob('*.java')) + [Path(__file__).resolve()]
    hashes = {path.relative_to(REPOSITORY).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
              for path in inputs}
    compile_command = ['javac', '--release', '17', '-encoding', 'UTF-8', '-Xlint:all', '-Werror',
                       '-d', 'build/app', '-sourcepath', 'src', 'src/breadcrumbs/Main.java']
    subprocess.run(compile_command, cwd=REPOSITORY, check=True, timeout=30)
    drive()
    save_html()
    save_png()
    assert hashes == {path.relative_to(REPOSITORY).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
                      for path in inputs}, 'Exercised source changed during capture'
    (OUTPUT / 'source-sha256.txt').write_text(''.join(f'{digest}  {path}\n'
        for path, digest in hashes.items()), encoding='utf-8')
    (OUTPUT / 'capture.json').write_text(json.dumps({'capturedAt': datetime.now().isoformat(),
        'kind': 'actual CLI output replay with input echoes', 'compileCommand': compile_command,
        'colorArguments': ['--color=always', '--rich-brand'], 'columns': [160, 80, 30],
        'sourceHashes': hashes, 'frames': frames}, indent=2), encoding='utf-8')
    print('PASS: actual create/B0/save/checkpoint/restart/resume/DLL/fallback walkthrough')
    print(OUTPUT / 'walkthrough.html')


if __name__ == '__main__':
    main()
