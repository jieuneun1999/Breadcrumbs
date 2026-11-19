$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$fixtures = Join-Path $repository ('build/b4-verification-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($fixtures) | Out-Null
$report = [Collections.Generic.List[string]]::new()
$report.Add("Started: $([DateTimeOffset]::Now.ToString('o'))")
$report.Add("OS: $([Environment]::OSVersion.VersionString)")
$inputs = @(Get-ChildItem -LiteralPath (Join-Path $repository 'src'), (Join-Path $repository 'tests'),
    (Join-Path $repository 'scripts') -File -Recurse)
$before = @($inputs | Sort-Object FullName | Get-FileHash -Algorithm SHA256)
function Check-Exit([string] $Name) {
    if ($LASTEXITCODE -ne 0) { throw "$Name failed with exit $LASTEXITCODE. Retained output: $fixtures" }
    $report.Add("PASS: $Name | exit 0")
}
Push-Location $repository
try {
    & $env:ComSpec /d /c scripts\build.bat
    Check-Exit 'Windows CMD production build'
    & javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/test -sourcepath 'src;tests' tests/breadcrumbs/AllTests.java tests/breadcrumbs/b4/B4Scenarios.java
    Check-Exit 'Strict Java 17 test compilation'
    $target = Join-Path $fixtures 'junction-target'
    $container = Join-Path $fixtures 'junction-container'
    [IO.Directory]::CreateDirectory($target) | Out-Null
    [IO.Directory]::CreateDirectory($container) | Out-Null
    $junction = Join-Path $container 'linked'
    & $env:ComSpec /d /c "mklink /J `"$junction`" `"$target`"" | Out-Null
    Check-Exit 'Synthetic Windows junction creation'
    $suite = & java "-Dbreadcrumbs.junction.fixture=$container" "-Dbreadcrumbs.b3Junction=$junction" "-Dbreadcrumbs.b4ExportJunction=$junction" -cp build/test breadcrumbs.AllTests 2>&1
    $suiteExit = $LASTEXITCODE
    [IO.File]::WriteAllText((Join-Path $fixtures 'tests.txt'), ($suite -join "`n"))
    if ($suiteExit -ne 0) { throw ($suite -join "`n") }
    foreach ($expected in @('PASS: all B1 test groups', 'PASS: all B2 test groups', 'PASS: all B3 test groups',
        'PASS: all B4 test groups', 'PASS: B4 actual Windows export junction rejection')) {
        if (-not ($suite -join "`n").Contains($expected)) { throw "Expected test output absent: $expected" }
    }
    $report.Add($suite -join "`n")
    $scenarios = & java -cp build/test breadcrumbs.b4.B4Scenarios 2>&1
    $scenarioExit = $LASTEXITCODE
    [IO.File]::WriteAllText((Join-Path $fixtures 'scenarios.txt'), ($scenarios -join "`n"))
    if ($scenarioExit -ne 0) { throw ($scenarios -join "`n") }
    $report.Add($scenarios -join "`n")
    # Launch scripts\run-test-console.bat through its absolute path from outside the repository.
    $exitInput = Join-Path $fixtures 'exit-input.txt'
    [IO.File]::WriteAllText($exitInput, "0`n")
    Push-Location $fixtures
    try {
        & $env:ComSpec /d /c "`"$repository\scripts\run-test-console.bat`" --no-color --storage-root=`"$fixtures\launcher-storage`" < `"$exitInput`"" > (Join-Path $fixtures 'launcher.txt')
        Check-Exit 'Windows launcher by absolute path from external directory'
    } finally { Pop-Location }
    $after = @($inputs | Sort-Object FullName | Get-FileHash -Algorithm SHA256)
    if (($before.Hash -join '|') -ne ($after.Hash -join '|')) { throw 'Verification inputs changed during the run.' }
    $report.Add('PASS: source/test/script hashes unchanged during verification')
    $relativeHashes = foreach ($item in $before) {
        $item.Hash.ToLowerInvariant() + '  ' + $item.Path.Substring($repository.Length + 1).Replace('\', '/')
    }
    [IO.File]::WriteAllText((Join-Path $fixtures 'source-sha256.txt'), ($relativeHashes -join "`n") + "`n")
    $report.Add("Completed: $([DateTimeOffset]::Now.ToString('o'))")
    [IO.File]::WriteAllText((Join-Path $fixtures 'summary.txt'), ($report -join "`n") + "`n")
    Write-Output ($report -join "`n")
    Write-Output "Evidence: $fixtures"
} finally { Pop-Location }
