"""Build/test the Windows Java application from WSL without PowerShell."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import uuid

REPO = Path(__file__).resolve().parents[1]
JDK = Path('/mnt/c/Program Files/Java/jdk-25/bin')
CSC = Path('/mnt/c/Windows/Microsoft.NET/Framework64/v4.0.30319/csc.exe')
CMD = '/mnt/c/Windows/System32/cmd.exe'
GROUPS = ['breadcrumbs.AllTests', 'breadcrumbs.cli.BreadSupportProcessTest',
          'breadcrumbs.storage.B7RecoveryProcessTest', 'breadcrumbs.b7.ProcessScenarios',
          'breadcrumbs.b7.ProjectMapProcessTest', 'breadcrumbs.b6.ProcessScenarios',
          'breadcrumbs.b5.ProcessScenarios', 'breadcrumbs.b4.B4Scenarios']


def linux_path(windows):
    return Path('/mnt') / windows[0].lower() / windows[3:].replace('\\', '/')


def inputs():
    result = {str(p.relative_to(REPO)): hashlib.sha256(p.read_bytes()).hexdigest()
            for folder in ['src', 'tests', 'scripts', 'tools'] for p in (REPO / folder).rglob('*')
            if p.is_file() and p.suffix in {'.java', '.cs', '.py', '.bat', '.cmd', '.ps1'}}
    result['run.bat'] = hashlib.sha256((REPO / 'run.bat').read_bytes()).hexdigest()
    return result


def run(command, cwd, log):
    with log.open('wb') as stream:
        result = subprocess.run([str(x) for x in command], cwd=cwd, stdout=stream,
                                stderr=subprocess.STDOUT, timeout=240, check=False)
    print(log.read_text(encoding='utf-8', errors='replace')[-5000:], flush=True)
    if result.returncode:
        raise RuntimeError(f'exit {result.returncode}: {command[0]} / {log}')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--focused', action='store_true')
    parser.add_argument('--preview', action='store_true')
    parser.add_argument('--native-check', action='store_true')
    parser.add_argument('--screen-check', action='store_true')
    parser.add_argument('--home-check', action='store_true', help='Native unified Home checkpoint before wider integration')
    parser.add_argument('--record', action='store_true', help='Record actual screen-check window with the installed FFmpeg')
    parser.add_argument('--ffmpeg', help='Existing Windows FFmpeg executable; no download or install')
    parser.add_argument('--app', action='store_true', help='Show original startup branding and integrated menus in the preview')
    args = parser.parse_args()
    if args.record and (not (args.screen_check or args.home_check) or not args.ffmpeg):
        parser.error('--record requires --screen-check or --home-check and --ffmpeg=<existing Windows executable>')
    os.chdir(REPO)
    token = uuid.uuid4().hex
    evidence = REPO / 'build' / ('bread-cmd-' + token)
    evidence.mkdir(parents=True)
    temp = subprocess.check_output([CMD, '/d', '/c', 'echo %TEMP%'], cwd='/mnt/c', text=True).strip()
    windows = temp + '\\breadcrumbs-symbol-' + token
    runtime = linux_path(windows)
    app, tests = runtime / 'build/app', runtime / 'build/test'
    app.mkdir(parents=True); tests.mkdir(parents=True)
    before = inputs()
    (evidence / 'inputs.json').write_text(json.dumps(before, indent=2))
    classes = windows + r'\build\app'
    manifest = {'source_head': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
                'runtime': windows, 'classes': classes, 'started': time.time(),
                'host': 'Windows Java via WSL; native preview hosted by CMD; no PowerShell', 'commands': []}
    (evidence / 'run.json').write_text(json.dumps(manifest, indent=2))
    compile_app = [JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-Xlint:all', '-Werror',
                   '-d', classes, '-sourcepath', 'src', 'src/breadcrumbs/Main.java']
    run(compile_app, REPO, evidence / 'compile-app.txt'); manifest['commands'].append(list(map(str, compile_app)))
    if CSC.exists():
        run([CSC, '/nologo', '/optimize+', '/warnaserror+', '/target:exe',
             '/out:' + classes + r'\bread-keys.exe', r'.\tools\bread-keys.cs'], REPO, evidence / 'compile-keys.txt')
    if args.preview or args.native_check or args.screen_check or args.home_check:
        if not CSC.exists():
            raise RuntimeError('Native preview requires the already installed optional Windows compiler')
        run([CSC, '/nologo', '/optimize+', '/warnaserror+', '/r:System.Drawing.dll', '/target:exe',
             '/out:' + windows + r'\bread-preview.exe', r'.\tools\bread-preview.cs'], REPO, evidence / 'compile-preview.txt')
        win_evidence = subprocess.check_output(['/usr/bin/wslpath', '-w', str(evidence)], text=True).strip()
        batch = '@echo off\r\nset "BREAD_JDK=' + str(JDK).replace('/mnt/c/', 'C:\\').replace('/', '\\') + '"\r\ncolor 07\r\n'
        if args.record:
            if not linux_path(args.ffmpeg).is_file():
                raise RuntimeError('Specified existing FFmpeg is unavailable')
            batch += 'set "BREAD_RECORD=1"\r\nset "BREAD_FFMPEG=' + args.ffmpeg + '"\r\n'
        batch += '"' + windows + '\\bread-preview.exe" "' + classes + '" "' + windows + '" "' + win_evidence + '"' + (' --home-auto' if args.home_check else ' --screen-auto' if args.screen_check else ' --auto' if args.native_check else ' --app' if args.app else '') + '\r\n'
        (runtime / 'preview.cmd').write_bytes(batch.encode('utf-8'))
        (runtime / 'launch.cmd').write_bytes(('@echo off\r\nstart ' + ('/min ' if args.native_check else '') + '"Bread Support symbols" "%SystemRoot%\\System32\\cmd.exe" /d /c "' + windows + '\\preview.cmd"\r\n').encode())
        subprocess.Popen([CMD, '/d', '/c', windows + r'\launch.cmd'], cwd=runtime,
                         stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                         stderr=subprocess.DEVNULL, start_new_session=True)
        if args.native_check or args.screen_check or args.home_check:
            deadline = time.monotonic() + (210 if args.record else 150)
            while not (evidence / 'checks.txt').exists() and time.monotonic() < deadline:
                time.sleep(.25)
            if not (evidence / 'checks.txt').exists():
                raise RuntimeError('Native CMD acceptance did not finish within its bounded deadline')
            if (evidence / 'error.txt').exists():
                raise RuntimeError((evidence / 'error.txt').read_text())
        manifest['preview'] = 'Native CMD window launched; actual console captures are separate evidence'
        if args.native_check or args.screen_check or args.home_check:
            manifest['result'] = 'PASS_NATIVE_AUTOMATED'
            manifest['scope'] = (evidence / 'checks.txt').read_text()
    else:
        targets = ['tests/breadcrumbs/AllTests.java', 'tests/breadcrumbs/cli/BreadSupportProcessTest.java',
                   'tests/breadcrumbs/storage/B7RecoveryProcessTest.java', 'tests/breadcrumbs/b7/ProcessScenarios.java',
                   'tests/breadcrumbs/b7/ProjectMapProcessTest.java', 'tests/breadcrumbs/b6/ProcessScenarios.java',
                   'tests/breadcrumbs/b5/ProcessScenarios.java', 'tests/breadcrumbs/b4/B4Scenarios.java']
        compile_tests = [JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-Xlint:all', '-Werror',
                         '-d', windows + r'\build\test', '-sourcepath', 'src;tests'] + targets
        run(compile_tests, REPO, evidence / 'compile-tests.txt')
        shutil.copytree(REPO / 'tests/fixtures', runtime / 'tests/fixtures')
        groups = ['breadcrumbs.cli.BreadSupportTest', 'breadcrumbs.cli.TerminalScreenTest', 'breadcrumbs.cli.BreadGazeTest', 'breadcrumbs.cli.UnifiedExperienceTest', 'breadcrumbs.cli.BreadSupportProcessTest'] if args.focused else GROUPS
        for group in groups:
            command = [JDK / 'java.exe', '-cp', windows + r'\build\test;' + classes, group]
            run(command, runtime, evidence / (group + '.txt')); manifest['commands'].append(list(map(str, command)))
        if not args.focused:
            launcher = runtime / 'launcher-test'
            launcher.mkdir()
            for name in ['src', 'tools']:
                shutil.copytree(REPO / name, launcher / name)
            (launcher / 'scripts').mkdir()
            for name in ['run.bat', 'scripts/build.bat']:
                shutil.copyfile(REPO / name, launcher / name)
            launcher_win = windows + r'\launcher-test'
            environment = os.environ.copy()
            environment['PATH'] = str(JDK).replace('/mnt/c/', 'C:\\').replace('/', '\\') + ';' + environment.get('PATH', '')
            environment['NO_COLOR'] = '1'
            command = [CMD, '/d', '/c', launcher_win + r'\run.bat', '--no-color', '--no-animation',
                       '--storage-root=' + launcher_win + r'\isolated-storage']
            result = subprocess.run(command, cwd=launcher, env=environment, input=b'0\r\n',
                                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=120, check=False)
            (evidence / 'production-launcher.txt').write_bytes(result.stdout)
            if result.returncode or b'Goodbye.' not in result.stdout or b'\x1b[' in result.stdout:
                raise RuntimeError('Exact run.bat/build.bat isolated fallback check failed')
            manifest['commands'].append(list(map(str, command)))
        if before != inputs():
            raise RuntimeError('Source inputs changed during verification; this run does not certify final inputs')
        manifest['result'] = 'PASS'
    if before != inputs():
        raise RuntimeError('Inputs changed during this run; final source binding is not proven')
    manifest['finished'] = time.time()
    manifest['app_sha256'] = {str(p.relative_to(app)): hashlib.sha256(p.read_bytes()).hexdigest() for p in app.rglob('*') if p.is_file()}
    (evidence / 'run.json').write_text(json.dumps(manifest, indent=2))
    print(evidence, flush=True)


if __name__ == '__main__':
    main()
