$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$evidence = Join-Path $repository 'build/verification'
New-Item -ItemType Directory -Force -Path $evidence | Out-Null
$started = [DateTimeOffset]::Now.ToString('o')
$report = [Collections.Generic.List[string]]::new()
$report.Add("Started: $started")
$report.Add("OS: $([Environment]::OSVersion.VersionString)")
$report.Add("Repository: $repository")
$inputs = @(Get-ChildItem -LiteralPath (Join-Path $repository 'src'),
    (Join-Path $repository 'tests'), (Join-Path $repository 'scripts') -File -Recurse)
$inputs += Get-Item -LiteralPath (Join-Path $repository 'run.bat')
$before = @($inputs | Sort-Object FullName | Get-FileHash -Algorithm SHA256)

function Invoke-Check([string] $Name, [string] $Command, [int] $ExpectedExit = 0) {
    $log = Join-Path $evidence ($Name + '.txt')
    & $env:ComSpec /d /c "$Command > `"$log`" 2>&1"
    $actual = $LASTEXITCODE
    $report.Add("$Name | exit $actual | $Command")
    if ($actual -ne $ExpectedExit) {
        throw "$Name failed with exit $actual. See $log"
    }
    return [IO.File]::ReadAllText($log)
}

function Require-Text([string] $Text, [string] $Fragment) {
    if (-not $Text.Contains($Fragment)) {
        throw "Expected output was absent: $Fragment"
    }
}

Push-Location $repository
try {
    $version = Invoke-Check 'java-version' 'java -version'
    $compiler = Invoke-Check 'javac-version' 'javac -version'
    $report.Add($version.Trim())
    $report.Add($compiler.Trim())
    $suite = Invoke-Check 'tests' 'scripts\test.bat'
    Require-Text $suite 'PASS: all B1 test groups'
    Require-Text $suite 'PASS: all B2 test groups'
    Require-Text $suite 'PASS: all B3 test groups'
    $report.Add($suite.Trim())
    $demo = Invoke-Check 'demo-root' 'run.bat --dsa < tests\cli-demo-input.txt'
    foreach ($fragment in @('Invalid menu option', 'Identifier must not be blank',
            'Position must be between', 'Loaded 5', 'Loaded 0', 'Duplicate checkpoint ID',
            'Deleted DEMO-MIDDLE', 'Success: CP-000001', 'USER_CONFIRMED',
            'Integrity valid', 'Goodbye')) {
        Require-Text $demo $fragment
    }
    $blank = Join-Path $evidence 'exit-input.txt'
    [IO.File]::WriteAllText($blank, "0`r`n")
    Push-Location $evidence
    try {
        $external = Invoke-Check 'demo-external' "`"$repository\run.bat`" --dsa < `"$blank`""
        Require-Text $external 'Goodbye'
    } finally {
        Pop-Location
    }
    $fixture = Join-Path $evidence 'path with spaces'
    New-Item -ItemType Directory -Force -Path $fixture | Out-Null
    foreach ($folder in @('src', 'scripts', 'tests')) {
        $sourceFolder = Join-Path $repository $folder
        $destinationFolder = Join-Path $fixture $folder
        New-Item -ItemType Directory -Force -Path $destinationFolder | Out-Null
        Get-ChildItem -LiteralPath $sourceFolder | Copy-Item -Destination $destinationFolder -Recurse -Force
    }
    Copy-Item -LiteralPath (Join-Path $repository 'run.bat') -Destination $fixture -Force
    $spaces = Invoke-Check 'demo-spaces' "`"$fixture\run.bat`" --dsa < `"$blank`""
    Require-Text $spaces 'Goodbye'
    $spacesSuite = Invoke-Check 'tests-spaces' "`"$fixture\scripts\test.bat`""
    Require-Text $spacesSuite 'PASS: all B1 test groups'
    Require-Text $spacesSuite 'PASS: all B2 test groups'
    Require-Text $spacesSuite 'PASS: all B3 test groups'
    $savedPath = $env:PATH
    try {
        $env:PATH = Join-Path $env:SystemRoot 'System32'
        $missing = Invoke-Check 'missing-jdk' 'run.bat' 1
        Require-Text $missing 'javac was not found'
    } finally {
        $env:PATH = $savedPath
    }
    $brokenMain = Join-Path $fixture 'src/breadcrumbs/Main.java'
    [IO.File]::WriteAllText($brokenMain, 'deliberately invalid Java')
    $broken = Invoke-Check 'compile-failure' "`"$fixture\run.bat`" --dsa < `"$blank`"" 1
    Require-Text $broken 'Compilation failed'
    if ($broken.Contains('Project Checkpoint Management')) {
        throw 'Launcher ran stale classes after a compiler failure.'
    }
    $after = @($inputs | Sort-Object FullName | Get-FileHash -Algorithm SHA256)
    for ($i = 0; $i -lt $before.Count; $i++) {
        if ($before[$i].Hash -ne $after[$i].Hash) {
            throw "Verification input changed: $($before[$i].Path)"
        }
    }
    $manifest = $before | ForEach-Object {
        "$($_.Hash)  $($_.Path.Substring($repository.Length + 1).Replace('\', '/'))"
    }
    [IO.File]::WriteAllLines((Join-Path $evidence 'source-sha256.txt'), $manifest)
    $report.Add('PASS: source SHA-256 hashes unchanged across verification.')
    $report.Add('PASS: root, external-directory, and paths-with-spaces CMD launch.')
    $report.Add('PASS: missing JDK and compilation failure return exit 1 without starting stale classes.')
    $report.Add('JDK 17 runtime execution NOT VERIFIED. Compilation targets --release 17.')
    $report.Add("Finished: $([DateTimeOffset]::Now.ToString('o'))")
    [IO.File]::WriteAllLines((Join-Path $evidence 'summary.txt'), $report)
    $report
} finally {
    Pop-Location
}
