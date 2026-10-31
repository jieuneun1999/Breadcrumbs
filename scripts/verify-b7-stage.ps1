param([ValidateSet('B7A','B7B','B7C','B7D','B7E')][string] $Stage = 'B7E')
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$destination = Join-Path $repository ('build/' + $Stage.ToLowerInvariant() + '-verification-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($destination) | Out-Null
function Read-Inputs {
    @(Get-ChildItem -LiteralPath (Join-Path $repository 'src'), (Join-Path $repository 'tests'), (Join-Path $repository 'scripts') -Recurse -File) + @(Get-Item -LiteralPath (Join-Path $repository 'run.bat')) | Sort-Object FullName | Get-FileHash -Algorithm SHA256
}
$before = @(Read-Inputs)
$started = [DateTimeOffset]::Now.ToString('o')
Push-Location $repository
try {
    $lines = & $env:ComSpec /d /c scripts\test-b7.bat 2>&1
    $runExit = $LASTEXITCODE
    $text = (($lines | ForEach-Object { $_.ToString() }) -join "`n") + "`n"
    [IO.File]::WriteAllText((Join-Path $destination 'tests.txt'), $text, [Text.UTF8Encoding]::new($false))
    $after = @(Read-Inputs)
    $beforeIdentity = ($before | ForEach-Object { $_.Path + '=' + $_.Hash }) -join '|'
    $afterIdentity = ($after | ForEach-Object { $_.Path + '=' + $_.Hash }) -join '|'
    if ($beforeIdentity -ne $afterIdentity) { throw 'Source/test/script inputs changed during verification. Retain logs; do not certify this run.' }
    $hashes = $before | ForEach-Object { $_.Hash.ToLowerInvariant() + '  ' + $_.Path.Substring($repository.Length + 1).Replace('\','/') }
    [IO.File]::WriteAllText((Join-Path $destination 'source-sha256.txt'), ($hashes -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
    $summary = "$Stage | Started $started | Completed $([DateTimeOffset]::Now.ToString('o'))`nOS $([Environment]::OSVersion.VersionString)`nExit $runExit | $($before.Count) input hashes unchanged`n"
    [IO.File]::WriteAllText((Join-Path $destination 'summary.txt'), $summary, [Text.UTF8Encoding]::new($false))
    Write-Output $summary
    Write-Output ($text -split "`n" | Where-Object { $_ -match 'PASS:.*(B7|all B[1-6])|assertions|AssertionError|failed' })
    Write-Output "Evidence: $destination"
    if ($runExit -ne 0) { throw "Stage regression failed, exit $runExit. Inspect $destination" }
} finally { Pop-Location }
