param([ValidateSet('B7A','B7B','B7C','B7D','B7E')][string] $Stage = 'B7E', [switch] $NativeWindows)
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$destination = Join-Path $repository ('build/' + $Stage.ToLowerInvariant() + '-verification-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($destination) | Out-Null
function Read-Inputs {
    @(Get-ChildItem -LiteralPath (Join-Path $repository 'src'), (Join-Path $repository 'tests'), (Join-Path $repository 'scripts') -Recurse -File) | Where-Object { $_.Extension -ne '.pyc' -and $_.DirectoryName -notmatch '[\\/]__pycache__([\\/]|$)' } | Sort-Object FullName | Get-FileHash -Algorithm SHA256
}
$before = @(Read-Inputs)
$started = [DateTimeOffset]::Now.ToString('o')
Push-Location $repository
$previousJavaOptions = $env:JDK_JAVA_OPTIONS
try {
    if ($NativeWindows) {
        if (-not $IsWindows) { throw 'NativeWindows requires Windows.' }
        $target = Join-Path $destination 'junction-target'
        $container = Join-Path $destination 'junction-container'
        [IO.Directory]::CreateDirectory($target) | Out-Null
        [IO.Directory]::CreateDirectory($container) | Out-Null
        $junction = Join-Path $container 'linked'
        New-Item -ItemType Junction -Path $junction -Target $target | Out-Null
        $env:JDK_JAVA_OPTIONS = "$previousJavaOptions -Dbreadcrumbs.junction.fixture=$container -Dbreadcrumbs.b3Junction=$junction -Dbreadcrumbs.b4ExportJunction=$junction"
    }
    $lines = & $env:ComSpec /d /c scripts\test-b7.bat 2>&1
    $runExit = $LASTEXITCODE
    $text = (($lines | ForEach-Object { $_.ToString() }) -join "`n") + "`n"
    [IO.File]::WriteAllText((Join-Path $destination 'tests.txt'), $text, [Text.UTF8Encoding]::new($false))
    if ($NativeWindows -and $runExit -eq 0) {
        $package = Join-Path $destination 'runnable folder with spaces'
        [IO.Directory]::CreateDirectory((Join-Path $package 'scripts')) | Out-Null
        Copy-Item -LiteralPath (Join-Path $repository 'src') -Destination $package -Recurse
        Copy-Item -LiteralPath (Join-Path $repository 'scripts/build.bat') -Destination (Join-Path $package 'scripts/build.bat')
        Copy-Item -LiteralPath (Join-Path $repository 'scripts\run-test-console.bat') -Destination (Join-Path $package 'scripts')
        $exitInput = Join-Path $destination 'exit.txt'
        [IO.File]::WriteAllText($exitInput, "0`n")
        Push-Location $destination
        try {
            $command = "`"$package\scripts\run-test-console.bat`" --no-color --storage-root=`"$destination\launcher storage`" < `"$exitInput`""
            $launcher = & $env:ComSpec /d /c $command 2>&1
            $launcherExit = $LASTEXITCODE
            [IO.File]::WriteAllText((Join-Path $destination 'launcher.txt'), ($launcher -join "`n") + "`n")
            if ($launcherExit -ne 0 -or ($launcher -join "`n") -notmatch 'Goodbye\.') { throw 'External-directory/path-with-spaces launcher failed.' }
        } finally { Pop-Location }
    }
    $after = @(Read-Inputs)
    $beforeIdentity = ($before | ForEach-Object { $_.Path + '=' + $_.Hash }) -join '|'
    $afterIdentity = ($after | ForEach-Object { $_.Path + '=' + $_.Hash }) -join '|'
    if ($beforeIdentity -ne $afterIdentity) { throw 'Source/test/script inputs changed during verification. Retain logs; do not certify this run.' }
    $hashes = $before | ForEach-Object { $_.Hash.ToLowerInvariant() + '  ' + $_.Path.Substring($repository.Length + 1).Replace('\','/') }
    [IO.File]::WriteAllText((Join-Path $destination 'source-sha256.txt'), ($hashes -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
    $summary = "$Stage | Started $started | Completed $([DateTimeOffset]::Now.ToString('o'))`nOS $([Environment]::OSVersion.VersionString)`nExit $runExit | $($before.Count) input hashes unchanged`nNativeWindows=$NativeWindows | junctions/ACL and unrelated-cwd space-path launcher exercised only when True`n"
    [IO.File]::WriteAllText((Join-Path $destination 'summary.txt'), $summary, [Text.UTF8Encoding]::new($false))
    Write-Output $summary
    Write-Output ($text -split "`n" | Where-Object { $_ -match 'PASS:.*(B7|all B[1-6])|assertions|AssertionError|failed' })
    Write-Output "Evidence: $destination"
    if ($runExit -ne 0) { throw "Stage regression failed, exit $runExit. Inspect $destination" }
} finally { $env:JDK_JAVA_OPTIONS = $previousJavaOptions; Pop-Location }
