"""Isolated Windows launch/error verification. Uses installed JDK; no global changes."""
import hashlib,json,os,shutil,subprocess,tempfile,time,uuid
from pathlib import Path
REPO=Path(__file__).resolve().parents[1]
def snapshot():
    paths=[p for folder in ['src','tests','scripts','tools'] for p in (REPO/folder).rglob('*') if p.is_file() and p.suffix in {'.java','.cs','.py','.bat','.ps1'}]
    paths+=list(REPO.glob('*.bat'))
    return {str(p.relative_to(REPO)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
def main():
    if os.name!='nt':raise SystemExit('Run with an existing Windows Python; application itself does not need Python.')
    token=uuid.uuid4().hex;ev=REPO/'build'/('classroom-launch-'+token);ev.mkdir(parents=True)
    runtime=Path(tempfile.mkdtemp(prefix='Breadcrumbs Classroom '+token[:8]+' '));app=runtime/'repo with spaces';app.mkdir()
    for name in ['src','tools']:shutil.copytree(REPO/name,app/name)
    (app/'scripts').mkdir()
    for name in ['run.bat','scripts/run-test-console.bat','scripts/DSA_DEMO.bat','scripts/build.bat']:shutil.copy2(REPO/name,app/name)
    initial=snapshot();(ev/'inputs.json').write_text(json.dumps(initial,indent=2),encoding='utf-8')
    jdk=Path('C:/Program Files/Java/jdk-25/bin');cmd=Path(os.environ['SystemRoot'])/'System32/cmd.exe'
    base=os.environ.copy();base['PATH']=str(jdk)+';'+str(cmd.parent)+';'+os.environ['SystemRoot'];base['NO_COLOR']='1';base['BREAD_SKIP_NATIVE']='1'
    # Fixtures use only this copied repository and per-process environment.
    results=[]
    def drive(name,launcher,args,values,env=None,success=True,need=(),absent=()):
        driver=runtime/(name+'.cmd');driver.write_bytes(('@echo off\r\ncall "'+str(app/launcher)+'" '+args+'\r\nexit /b %errorlevel%\r\n').encode())
        p=subprocess.run([str(cmd),'/d','/c',str(driver)],cwd=runtime,env=env or base,input=values.encode(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=100)
        text=p.stdout.decode('utf-8',errors='replace');(ev/(name+'.txt')).write_text(text,encoding='utf-8')
        assert (p.returncode==0)==success,(name,p.returncode,text[-1000:])
        assert all(x in text for x in need) and all(x not in text for x in absent),(name,text[-1600:])
        results.append({'case':name,'exit':p.returncode,'result':'PASS','command':['cmd.exe','/d','/c',str(driver)]})
    drive('space-path-unrelated-cwd','scripts/run-test-console.bat','--no-color --classroom','6\r\n10\r\n0\r\n',need=['The Bread','Five SYNTHETIC','Size: 5','Auxiliary space:','Goodbye'])
    drive('classroom-wrapper','scripts/DSA_DEMO.bat','--no-color','9\r\n0\r\n',need=['HEAD=DEMO-000001','TAIL=DEMO-000005','The Bread'])
    drive('scripted-console','scripts/run-test-console.bat','--no-color --storage-root="'+str(runtime/'isolated storage')+'"','0\r\n',need=['Breadcrumbs >','Goodbye.'])
    drive('missing-helper-numbered','scripts/run-test-console.bat','--classroom --no-color','4\r\nDEMO-000003\r\n0\r\n',need=['Portable numbered input','Found DEMO-000003'])
    missing=base.copy();missing['PATH']=str(cmd.parent)+';'+os.environ['SystemRoot']
    drive('missing-java','run.bat','','',missing,False,need=['Java tools were not found','Both java and javac'],absent=['Breadcrumbs >'])
    drive('launcher-readable-failure','run.bat','','\r\n',missing,False,need=['Java tools were not found','could not start','Press any key'])
    old=base.copy();old['PATH']='C:/Program Files/Java/jre-1.8/bin;'+base['PATH']
    if Path('C:/Program Files/Java/jre-1.8/bin/java.exe').exists():drive('old-runtime','run.bat','','',old,False,need=['Java 17 or newer is required'],absent=['Breadcrumbs >'])
    # Old class output would prove stale execution after an intentionally broken build.
    source=app/'src/breadcrumbs/Main.java';original=source.read_bytes();source.write_bytes(original+b'\nTHIS_IS_AN_ISOLATED_COMPILE_FAILURE\n')
    drive('compile-failure-no-stale','run.bat','','\r\n',success=False,need=['Compilation failed','old classes will not be run'],absent=['Five SYNTHETIC','Breadcrumbs >'])
    source.write_bytes(original)
    drive('full-ui-rejects-missing-helper','run.bat','','\r\n',success=False,
          need=['colored interface needs the Windows keyboard helper'],absent=['Breadcrumbs >'])
    native_console=base.copy();native_console.pop('BREAD_SKIP_NATIVE')
    drive('full-ui-rejects-redirected-console','run.bat','','\r\n',native_console,False,
          need=['needs an interactive Windows console'],absent=['Breadcrumbs >'])
    helper=app/'tools/bread-keys.cs';original_helper=helper.read_bytes();helper.write_text('deliberately invalid C sharp',encoding='utf-8');native=base.copy();native.pop('BREAD_SKIP_NATIVE')
    drive('helper-build-failure-portable','scripts/run-test-console.bat','--classroom --no-color','10\r\n0\r\n',native,need=['Optional keyboard helper could not be built','Size: 5','Goodbye'])
    assert not (app/'build/app/bread-keys.exe').exists()
    drive('full-ui-rejects-failed-helper-build','run.bat','','\r\n',native,False,
          need=['colored interface needs the Windows keyboard helper'],absent=['Breadcrumbs >'])
    helper.write_bytes(original_helper)
    # JDK-only menu and local storage remain usable with no requested network service.
    assert initial==snapshot(),'Inputs changed during launcher verification'
    version=subprocess.check_output([str(jdk/'java.exe'),'--version'],text=True)
    result={'status':'PASS','source':'See inputs.json for exercised source hashes','runtime':str(runtime),'java':version,'cases':results,'no_network':'All launch/build commands use only local files and installed JDK; no network service/download invoked. OS network blocking is NOT VERIFIED.','explorer_double_click':'Separate actual UI check; these cases are CMD child-process tests.','inputs':len(initial)}
    (ev/'run.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps({'evidence':str(ev),'cases':len(results),'result':'PASS'},indent=2))
if __name__=='__main__':main()
