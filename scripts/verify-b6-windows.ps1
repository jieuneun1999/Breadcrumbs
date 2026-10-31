$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$fixtures = Join-Path $repository ('build/b6-verification-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($fixtures) | Out-Null
$inputs = @(Get-ChildItem -LiteralPath (Join-Path $repository 'src'), (Join-Path $repository 'tests'), (Join-Path $repository 'scripts') -File -Recurse) + (Get-Item -LiteralPath (Join-Path $repository 'run.bat'))
$before = @($inputs | Sort-Object FullName | Get-FileHash -Algorithm SHA256)
$report = [Collections.Generic.List[string]]::new()
$report.Add("Started: $([DateTimeOffset]::Now.ToString('o'))")
$report.Add("OS: $([Environment]::OSVersion.VersionString)")
function Check-Exit([string] $Name) {
    if ($LASTEXITCODE -ne 0) { throw "$Name failed with exit $LASTEXITCODE. Evidence: $fixtures" }
    $report.Add("PASS: $Name | exit 0")
}
Push-Location $repository
try {
    & $env:ComSpec /d /c scripts\build.bat
    Check-Exit 'Windows production build'
    & javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/test -sourcepath 'src;tests' tests/breadcrumbs/AllTests.java tests/breadcrumbs/b6/ProcessScenarios.java tests/breadcrumbs/b5/ProcessScenarios.java tests/breadcrumbs/b4/B4Scenarios.java
    Check-Exit 'Strict Java 17 target test compilation'
    $target = Join-Path $fixtures 'junction-target'
    $container = Join-Path $fixtures 'junction-container'
    [IO.Directory]::CreateDirectory($target) | Out-Null
    [IO.Directory]::CreateDirectory($container) | Out-Null
    $junction = Join-Path $container 'linked'
    New-Item -ItemType Junction -Path $junction -Target $target | Out-Null
    $suite = & java "-Dbreadcrumbs.junction.fixture=$container" "-Dbreadcrumbs.b3Junction=$junction" "-Dbreadcrumbs.b4ExportJunction=$junction" -cp build/test breadcrumbs.AllTests 2>&1
    $suiteExit = $LASTEXITCODE
    [IO.File]::WriteAllText((Join-Path $fixtures 'tests.txt'), ($suite -join "`n") + "`n")
    if ($suiteExit -ne 0) { throw ($suite -join "`n") }
    $report.Add($suite -join "`n")
    foreach ($runner in @('breadcrumbs.b6.ProcessScenarios', 'breadcrumbs.b5.ProcessScenarios', 'breadcrumbs.b4.B4Scenarios')) {
        $result = & java -cp build/test $runner 2>&1
        $runnerExit = $LASTEXITCODE
        [IO.File]::WriteAllText((Join-Path $fixtures ($runner + '.txt')), ($result -join "`n") + "`n")
        if ($runnerExit -ne 0) { throw ($result -join "`n") }
        $report.Add($result -join "`n")
    }
    $exitInput = Join-Path $fixtures 'exit-input.txt'
    [IO.File]::WriteAllText($exitInput, "0`n")
    Push-Location $fixtures
    try {
        & $env:ComSpec /d /c "`"$repository\run.bat`" --no-color --storage-root=`"$fixtures\launcher-storage`" < `"$exitInput`"" > (Join-Path $fixtures 'launcher.txt')
        Check-Exit 'External-directory Windows launcher'
    } finally { Pop-Location }
    $after = @($inputs | Sort-Object FullName | Get-FileHash -Algorithm SHA256)
    if (($before.Hash -join '|') -ne ($after.Hash -join '|')) { throw 'Verification inputs changed during the run.' }
    $report.Add('PASS: all source/test/script hashes unchanged during verification')
    $hashes = foreach ($item in $before) { $item.Hash.ToLowerInvariant() + '  ' + $item.Path.Substring($repository.Length + 1).Replace('\', '/') }
    [IO.File]::WriteAllText((Join-Path $fixtures 'source-sha256.txt'), ($hashes -join "`n") + "`n")
    $report.Add("Completed: $([DateTimeOffset]::Now.ToString('o'))")
    [IO.File]::WriteAllText((Join-Path $fixtures 'summary.txt'), ($report -join "`n") + "`n")
    Write-Output ($report -join "`n")
    Write-Output "Evidence: $fixtures"
} finally { Pop-Location }
