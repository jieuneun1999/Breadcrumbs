"""Real populated-project simulation. Preflight Main, then record native ProjectConsole.

All people, plans and reported events are synthetic. No source file is executed.
Reading pauses belong to the input harness, never the application operations.
"""
from pathlib import Path
import argparse
import ctypes
import csv
import hashlib
import importlib.util
import json
import os
import re
import subprocess
import time
import zipfile
from xml.sax.saxutils import escape

REPO = Path(__file__).resolve().parents[1]
TITLE = 'Breadcrumbs - community food drive simulation'
OVERVIEW = '# Objective\nPrepare 100 food packs for a community pickup day.\n# Current State\nPlanning is underway; the hall booking needs confirmation.\n# Next Action\nConfirm the hall booking with the coordinator.\n'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def classes():
    return {str(p.relative_to(REPO)): digest(p) for p in (REPO / 'build/app').rglob('*.class')}


def fixture(source):
    source.mkdir()
    (source / '00-overview.md').write_text(OVERVIEW, encoding='utf-8')
    sections = [
        ('Purpose', 'Prepare 100 food packs for a Saturday community pickup. Each pack contains rice, canned food and basic cooking supplies.'),
        ('Venue', 'The community hall is the preferred pickup site. Keep the entrance clear and arrange a separate area for volunteers to assemble packs.'),
        ('Delivery', 'Ask a delivery company for a written quote. Confirm the pickup address, vehicle capacity and delivery window before committing funds.'),
        ('Volunteers', 'Plan two shifts of six volunteers. Assign one coordinator to answer questions and keep the packing table supplied.'),
        ('Purchasing', 'Compare unit prices before buying supplies. Keep receipts together and record changes to the budget.'),
        ('Packing', 'Count every item against the pack list. Inspect seals and dates before placing food into a bag.'),
        ('Accessibility', 'Provide a clear route from the entrance to the pickup table. Reserve seating for people who need a rest.'),
        ('Communication', 'Share the confirmed pickup window after the venue and delivery arrangements are agreed. Keep a contact list for team coordinators.'),
        ('Budget', 'Use a planning allowance of 25000 pesos. A delivery quote is still needed, so the transport allowance is provisional.'),
        ('Contingency', 'If delivery is delayed, notify the coordinator before changing the pickup window. Do not promise a revised time until transport is confirmed.'),
        ('Handover', 'Leave the latest count, unresolved questions and next action in the project notes so the next coordinator can continue.'),
        ('Closeout', 'Reconcile receipts and remaining supplies after pickup. Record lessons and any commitments that still need follow-up.'),
    ]
    paragraphs = ['SIMULATED PROJECT. All names, schedules, amounts and reported events are invented for the Breadcrumbs demonstration.', 'Community food drive: populated project plan']
    for heading, detail in sections:
        paragraphs.extend([heading, detail, 'Coordinator check: confirm the arrangement with the responsible person before treating this plan as current.',
                           'Working note: record what changed, what is still waiting and the next small step after each planning session.',
                           'Acceptance check: retain the written agreement or receipt when one becomes available. A plan alone does not prove the activity happened.'])
    body = ''.join('<w:p><w:r><w:t>' + escape(p) + '</w:t></w:r></w:p>' for p in paragraphs)
    xml = '<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>' + body + '</w:body></w:document>'
    with zipfile.ZipFile(source / 'food-drive-plan.docx', 'w', zipfile.ZIP_DEFLATED) as doc:
        doc.writestr('[Content_Types].xml', '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>')
        doc.writestr('_rels/.rels', '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>')
        doc.writestr('word/document.xml', xml)
    with (source / 'coordination.csv').open('w', encoding='utf-8', newline='') as table:
        writer = csv.writer(table)
        writer.writerow(['objective', 'owner', 'next_action'])
        writer.writerows((heading, 'Demo coordinator', detail.split('.')[0]) for heading, detail in sections)
    (source / 'SIMULATION.txt').write_text('Synthetic example only. No real event, contact information or existing Breadcrumbs session.\n', encoding='utf-8')
    return len(paragraphs)


def preflight(folder):
    folder.mkdir(parents=True, exist_ok=False)
    paragraphs = fixture(folder / 'source')
    spec = importlib.util.spec_from_file_location('walkthrough', REPO / 'scripts/record-b7-walkthrough.py')
    driver = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(driver)
    driver.FAST = True
    s = driver.Session(folder, '01-start')
    for marker, value in [
        ('Breadcrumbs > ', '2'), ('Entry mode: ', '2'), ('Project display name: ', 'Community food drive (simulation)'),
        ('Project type', '3'), ('Existing local project directory: ', str(folder / 'source')), ('Approve bounded', 'yes'),
        ('Overview action', '2'), ('Map > ', '1'), ('Section > ', '1'), ('Record > ', 't'),
        ('Record > ', '0'), ('Section > ', '0'), ('Map > ', '0'),
        ('Overview action', '4'), ('Confirm this starting record', 'yes'), ('Save this project', 'yes'), ('Breadcrumbs > ', '7'),
        ('Workspace > ', '2'), ('Progress summary', 'Booked the hall and drafted the volunteer schedule'),
        ('Blockers or pending', 'Waiting for the delivery quote'), ('Next action', 'Ask the delivery company for a quote'),
        ('Update action', 'yes'), ('Workspace > ', '13'), ('Map > ', '3'), ('Section > ', '0'), ('Map > ', '0'),
        ('Workspace > ', '1'), ('Workspace > ', '0'), ('Breadcrumbs > ', '0')]:
        s.answer(marker, value)
    s.finish()
    assert '0 reading problems' in ' '.join(s.raw.split()), 'Generated fixture should be readable'
    change(folder / 'source')
    s = driver.Session(folder, '02-return')
    for marker, value in [
        ('Breadcrumbs > ', '1'), ('Project number (0 = cancel): ', '1'), ('Breadcrumbs > ', '7'),
        ('Workspace > ', '13'), ('Map > ', '0'), ('Workspace > ', '10'), ('Notes > ', '1'),
        ('Note category', '3'), ('Note content', 'The hall coordinator said two extra tables are available'),
        ('Information source', 'Simulated planning call'), ('Optional reference', 'Planning call notes'), ('Privacy [', ''),
        ('Add note [', 'c'), ('Allow words', 'yes'), ('Allow this note', 'yes'), ('Also use this note', ''),
        ('Record a progress update', ''), ('Notes > ', '0'), ('Workspace > ', '5'), ('Search > ', '4'), ('Search > ', '1'),
        ('Search term', 'tables'), ('Item number to read', '1'), ('Show this reviewed', 'yes'), ('Search > ', '0'),
        ('Workspace > ', '4'), ('Read your project folder', 'yes'), ('Review action', '1'), ('Mark these changes', 'yes'),
        ('Workspace > ', '13'), ('Map > ', '1'), ('Section > ', '0'), ('Map > ', '0'),
        ('Workspace > ', '1'), ('Workspace > ', '11'), ('Handoff > ', '6'), ('Progress update numbers', '1'),
        ('New full folder path', str(folder / 'portable')), ('Create this new handoff', 'yes'), ('Handoff > ', '0'),
        ('Workspace > ', '0'), ('Breadcrumbs > ', '0')]:
        s.answer(marker, value)
    s.finish()
    assert (folder / 'portable/HANDOFF.md').exists()
    for name in ['01-start', '02-return']:
        events = json.loads((folder / (name + '-events.json')).read_text(encoding='utf-8'))
        answers, pauses, previous = [], [], ''
        for event in events:
            if event['kind'] == 'stdout':
                previous = event['text']
            else:
                visible = driver.SGR.sub('', previous).rstrip()
                if visible.endswith(('Breadcrumbs >', 'Workspace >', 'Search >', 'Handoff >', 'Notes >', 'Update action:', 'Review action:', 'Overview action:', 'Map >', 'Section >', 'Record >')):
                    selected = event['text'].strip()
                    assert re.search(r'\[' + re.escape(selected) + r'\]', visible, re.IGNORECASE), 'Demo selected an action that was not displayed: ' + selected
                answers.append(event['text'])
                pauses.append(12000 if 'Project summary' in previous or 'Project overview / What we found' in previous else 10000 if 'Review progress update' in previous or 'File comparison' in previous else 8000 if 'Search results' in previous else 6500 if visible.endswith(('Map >', 'Section >', 'Record >')) else 4500 if len(previous) > 700 else 1600)
        pauses[0] = 6500
        (folder / (name + '.input.txt')).write_text(''.join(answers).replace(str(folder / 'portable'), str(folder / 'native-portable')), encoding='utf-8')
        (folder / (name + '.pacing.txt')).write_text('\n'.join(map(str, pauses)) + '\n', encoding='utf-8')
    (folder / 'source/00-overview.md').write_text(OVERVIEW, encoding='utf-8')
    (folder / 'preflight.json').write_text(json.dumps({'simulation': True, 'docx_paragraphs': paragraphs, 'class_hashes': classes(), 'source_hashes': {p.name: digest(p) for p in (folder / 'source').iterdir()}}, indent=2), encoding='utf-8')


def change(source):
    (source / '00-overview.md').write_text(OVERVIEW.replace('Confirm the hall booking with the coordinator.', 'Ask the delivery company for a written quote.'), encoding='utf-8')


def record(folder):
    receipt = json.loads((folder / 'preflight.json').read_text(encoding='utf-8'))
    assert receipt['class_hashes'] == classes(), 'Production classes changed after preflight'
    assert receipt['source_hashes'] == {p.name: digest(p) for p in (folder / 'source').iterdir()}, 'Simulation inputs changed'
    assert not (folder / 'native-app').exists() and not (folder / 'walkthrough.mp4').exists(), 'Capture is create-only'
    kernel = ctypes.windll.kernel32
    kernel.SetConsoleTitleW(TITLE)
    handle, mode = kernel.GetStdHandle(-11), ctypes.c_ulong()
    if not kernel.GetConsoleMode(handle, ctypes.byref(mode)) or not kernel.SetConsoleMode(handle, mode.value | 4):
        raise RuntimeError('Native console VT is unavailable')
    env = os.environ.copy()
    env['BREADCRUMBS_NATIVE_VT'] = '1'
    with (folder / 'capture.txt').open('w', encoding='utf-8') as log:
        capture = subprocess.Popen(['ffmpeg', '-hide_banner', '-loglevel', 'warning', '-f', 'gdigrab', '-framerate', '24', '-i', 'title=' + TITLE, '-an', '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '22', '-pix_fmt', 'yuv420p', '-vf', 'pad=ceil(iw/2)*2:ceil(ih/2)*2', str(folder / 'walkthrough.mp4')], stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=log)
        try:
            time.sleep(2)
            if capture.poll() is not None:
                raise RuntimeError('Window capture failed')
            print('SIMULATED PROJECT | Real Breadcrumbs app, populated project files, scripted input.\nNo prior session. No real people or events. Reading pauses are in the demo only.\n', flush=True)
            for number, name in enumerate(['01-start', '02-return']):
                if number:
                    print('\nRETURNING TO WORK | The first app process has exited. Reopen the project in a new process.\nThe demo changes one project note so you can review an actual file change.\n', flush=True)
                    change(folder / 'source')
                    time.sleep(5)
                result = subprocess.run(['java', '-cp', str(REPO / 'build/app') + os.pathsep + str(REPO / 'build/native-demo'), 'breadcrumbs.cli.NativeWalkthrough', str(folder / (name + '.input.txt')), str(folder / 'native-app'), str(folder / (name + '-native.txt')), str(folder / (name + '.pacing.txt'))], cwd=REPO, env=env, timeout=360)
                if result.returncode:
                    raise RuntimeError('Native session failed')
                text = (folder / (name + '-native.txt')).read_text(encoding='utf-8')
                assert 'Goodbye.' in text and 'End of input' not in text
            print('\nDEMO COMPLETE | Your update was saved and reopened. A reported note was found in search.\nYou checked a changed file and exported a handoff folder you can open without Breadcrumbs.\nThis was a simulation, not verification of real community work.\n', flush=True)
            time.sleep(7)
            assert classes() == receipt['class_hashes']
            for name, original in receipt['source_hashes'].items():
                if name != '00-overview.md':
                    assert digest(folder / 'source' / name) == original, 'App must not change imported files'
            assert (folder / 'native-portable/checkpoints/CP-000001.md').exists()
            (folder / 'native-manifest.json').write_text(json.dumps({'simulation': True, 'capture': 'real native production ProjectConsole, two Java processes, scripted Reader, process-local verified VT', 'class_hashes': classes(), 'source_hashes_after': {p.name: digest(p) for p in (folder / 'source').iterdir()}, 'video': 'walkthrough.mp4'}, indent=2), encoding='utf-8')
        finally:
            if capture.poll() is None:
                capture.stdin.write(b'q\n')
                capture.stdin.flush()
                capture.wait(timeout=25)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--record', action='store_true')
    args = parser.parse_args()
    (record if args.record else preflight)(args.output.resolve())
