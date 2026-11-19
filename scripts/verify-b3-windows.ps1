$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$fixtures = Join-Path $repository ('build/b3-verification-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($fixtures) | Out-Null
$storage = Join-Path $fixtures 'app storage'
$report = [Collections.Generic.List[string]]::new()
$report.Add("Started: $([DateTimeOffset]::Now.ToString('o'))")
$report.Add("OS: $([Environment]::OSVersion.VersionString)")
$inputs = @(Get-ChildItem -LiteralPath (Join-Path $repository 'src'), (Join-Path $repository 'tests'),
    (Join-Path $repository 'scripts') -File -Recurse)
$before = @($inputs | Sort-Object FullName | Get-FileHash -Algorithm SHA256)

function Require-Text([string] $Text, [string] $Fragment) {
    if (-not $Text.Contains($Fragment)) { throw "Expected output missing: $Fragment" }
}

function Invoke-App([string] $Name, [string] $InputText, [string] $Extra = '--no-color', [bool] $NoColorEnvironment = $false, [int] $Columns = 80) {
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = 'java'
    $info.WorkingDirectory = $repository
    $info.Arguments = "-cp build/app breadcrumbs.Main `"--storage-root=$storage`" $Extra"
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = [Text.Encoding]::UTF8
    $info.EnvironmentVariables['COLUMNS'] = [string]$Columns
    $info.EnvironmentVariables.Remove('NO_COLOR')
    if ($NoColorEnvironment) { $info.EnvironmentVariables['NO_COLOR'] = '1' }
    $process = [Diagnostics.Process]::Start($info)
    $outputTask = $process.StandardOutput.ReadToEndAsync()
    $errorTask = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.Write($InputText.Replace("`r`n", "`n"))
    $process.StandardInput.Close()
    if (-not $process.WaitForExit(30000)) { $process.Kill(); throw "$Name exceeded 30s fixture deadline" }
    $text = $outputTask.Result + $errorTask.Result
    [IO.File]::WriteAllText((Join-Path $fixtures "$Name.txt"), $text)
    if ($process.ExitCode -ne 0) { throw "$Name exit $($process.ExitCode). See retained output." }
    $report.Add("PASS: $Name | separate Java process exit 0")
    return $text
}

Push-Location $repository
try {
    & $env:ComSpec /d /c scripts\build.bat
    if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
    & $env:ComSpec /d /c scripts\test.bat > (Join-Path $fixtures 'tests.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
    $target = Join-Path $fixtures 'junction-target'
    $junctionContainer = Join-Path $fixtures 'junction-container'
    [IO.Directory]::CreateDirectory($target) | Out-Null
    [IO.Directory]::CreateDirectory($junctionContainer) | Out-Null
    $junction = Join-Path $junctionContainer 'linked'
    & $env:ComSpec /d /c "mklink /J `"$junction`" `"$target`"" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Junction fixture creation unavailable' }
    $suite = & java "-Dbreadcrumbs.b3Junction=$junction" "-Dbreadcrumbs.junction.fixture=$junctionContainer" -cp build/test breadcrumbs.AllTests 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($suite -join "`n") }
    $suiteText = $suite -join "`n"
    Require-Text $suiteText 'PASS: all B3 test groups'
    Require-Text $suiteText 'PASS: B3 actual storage junction rejection'
    Require-Text $suiteText 'PASS: B3 actual Windows ACL read/save denial'
    $report.Add($suiteText)
    $report.Add((& $env:ComSpec /d /c 'java -version 2>&1' | Out-String).Trim())

    $new = Invoke-App 'a-new-create-save' "2`n1`nSchool Planner`nACADEMIC`nWrite report`nReport`nReview scope`n`n`n`nyes`nyes`n0`n"
    Require-Text $new 'Saved. Verified local file readback.'
    $files = @(Get-ChildItem -LiteralPath (Join-Path $storage 'workspaces') -Filter state.txt -Recurse -File)
    if ($files.Count -ne 1) { throw 'Expected exactly one saved B0' }
    $firstFile = $files[0].FullName
    $firstId = Split-Path (Split-Path $firstFile -Parent) -Leaf
    $firstHash = (Get-FileHash -LiteralPath $firstFile).Hash
    $resume = Invoke-App 'a-new-restart' "1`n1`n3`n1`n`n7`n0`n0`n"
    Require-Text $resume $firstId
    Require-Text $resume 'Project native checkpoints: 0'
    if ($firstHash -ne (Get-FileHash -LiteralPath $firstFile).Hash) { throw 'Read-only reopen changed persisted B0' }

    $cp = Invoke-App 'c-checkpoint-save' "1`n1`n3`n2`nFirst confirmed work`nDraft ready`nWrite report`nUNKNOWN`n`nReview draft`nChoose short sections`nStudent`n`nyes`n0`n0`n"
    Require-Text $cp 'Confirmed CP-000001'
    $cpResume = Invoke-App 'c-checkpoint-restart' "1`n1`n3`n3`n4`n8`n7`n0`n0`n"
    foreach ($text in @('CP-000001', 'First confirmed work', 'History backward:', 'Integrity: true', 'Next native number: 2')) {
        Require-Text $cpResume $text
    }

    $source = Join-Path $fixtures 'readme project'
    [IO.Directory]::CreateDirectory($source) | Out-Null
    [IO.File]::WriteAllText((Join-Path $source 'README.md'), "# Objective`nWrite a thesis.`n# Next Steps`nReview outline.`n")
    [IO.File]::WriteAllText((Join-Path $source '.env'), 'SYNTHETIC_SECRET=must-not-copy')
    $sourceBefore = (Get-FileHash -LiteralPath (Join-Path $source 'README.md')).Hash
    $import = Invoke-App 'b-readme-import' "2`n2`nImported Thesis`nACADEMIC`n$source`nyes`nc`nu`nyes`nyes`n0`n"
    Require-Text $import 'Saved. Verified'
    $files = @(Get-ChildItem -LiteralPath (Join-Path $storage 'workspaces') -Filter state.txt -Recurse -File)
    if ($files.Count -ne 2) { throw 'Expected two independent saved workspaces' }
    $secondFile = ($files | Where-Object FullName -ne $firstFile)[0].FullName
    $secondId = Split-Path (Split-Path $secondFile -Parent) -Leaf
    if ($sourceBefore -ne (Get-FileHash -LiteralPath (Join-Path $source 'README.md')).Hash) { throw 'Import changed source bytes' }
    # Disappear the synthetic source to prove startup/reopen uses saved metadata without rescanning it.
    Move-Item -LiteralPath $source -Destination ($source + '-gone')
    $sorted = @($firstId, $secondId) | Sort-Object
    $firstIndex = [Array]::IndexOf($sorted, $firstId) + 1
    $secondIndex = [Array]::IndexOf($sorted, $secondId) + 1
    $importResume = Invoke-App 'b-readme-restart-source-gone' "1`n$secondIndex`n3`n1`nyes`n7`n0`n0`n"
    foreach ($text in @('Imported Thesis', 'Source: README.md line 2', 'SOURCE_REPORTED', 'CONFIRMED', 'Freshness UNKNOWN')) {
        Require-Text $importResume $text
    }
    $switch = Invoke-App 'd-switch-workspaces' "1`n$firstIndex`n3`n3`n6`n$secondIndex`n3`n3`n0`n0`n"
    Require-Text $switch "School Planner | $firstId"
    Require-Text $switch "Imported Thesis | $secondId"
    Require-Text $switch 'No native checkpoint. Review B0'

    [IO.File]::WriteAllText($secondFile, 'corrupt disposable fixture')
    $bad = Invoke-App 'e-corrupt-load' "1`n$firstIndex`n1`n$secondIndex`n3`n3`n0`n0`n"
    foreach ($text in @('unreadable saved workspace', 'Storage/input operation failed', 'First confirmed work')) { Require-Text $bad $text }
    $plain = Invoke-App 'f-plain-startup' "0`n" ''
    if ($plain.Contains([string][char]27)) { throw 'Redirected output emitted ANSI' }
    $color = Invoke-App 'f-gold-startup' "0`n" '--color=always'
    Require-Text $color "$([char]27)[1;38;5;214m"
    Require-Text $color "$([char]27)[0m"
    $rich = Invoke-App 'f-rich-startup' "0`n" '--color=always --rich-brand' $false 160
    Require-Text $rich "$([char]27)[1;38;2;"
    Require-Text $rich 'Leave context behind. Pick up where work stopped.'
    Require-Text $rich 'Saved projects: 2'
    $richNarrow = Invoke-App 'f-rich-narrow' "0`n" '--color=always --rich-brand'
    Require-Text $richNarrow "$([char]27)[1;38;5;214m"
    if ($richNarrow.Contains("$([char]27)[1;38;2;")) {
        throw 'Narrow rich request did not fall back to ASCII'
    }
    $richRedirected = Invoke-App 'f-rich-redirected' "0`n" '--rich-brand' $false 160
    if ($richRedirected.Contains([string][char]27)) {
        throw 'Redirected rich request did not fall back to plain ASCII'
    }
    $override = Invoke-App 'f-no-color-override' "0`n" '--color=always --rich-brand --no-color' $false 160
    if ($override.Contains([string][char]27)) {
        throw '--no-color did not override always and rich art'
    }
    $noColorEnvironment = Invoke-App 'f-no-color-environment' "0`n" '--color=always --rich-brand' $true 160
    if ($noColorEnvironment.Contains([string][char]27)) {
        throw 'NO_COLOR did not override always and rich art'
    }
    $exitInput = Join-Path $fixtures 'exit-input.txt'
    [IO.File]::WriteAllText($exitInput, "1`n$firstIndex`n3`n7`n0`n0`n")
    Push-Location $fixtures
    try {
        $launcherLog = Join-Path $fixtures 'launcher-project.txt'
        & $env:ComSpec /d /c "`"$repository\scripts\run-test-console.bat`" `"--storage-root=$storage`" --no-color < `"$exitInput`" > `"$launcherLog`" 2>&1"
        if ($LASTEXITCODE -ne 0) { throw 'External-path project launcher failed' }
        $launcherText = [IO.File]::ReadAllText($launcherLog)
        Require-Text $launcherText "App storage: $storage"
        Require-Text $launcherText 'Saved projects: 2'
        Require-Text $launcherText 'Goodbye.'
        $report.Add('PASS: actual scripts\run-test-console.bat from external directory with quoted storage path and --no-color.')
    } finally { Pop-Location }
    foreach ($file in @(Get-ChildItem -LiteralPath $fixtures -Filter '*.txt' -File)) {
        if ([IO.File]::ReadAllText($file.FullName).Contains('must-not-copy')) { throw 'Sensitive fixture value reached output' }
    }
    $after = @($inputs | Sort-Object FullName | Get-FileHash -Algorithm SHA256)
    for ($i = 0; $i -lt $before.Count; $i++) {
        if ($before[$i].Hash -ne $after[$i].Hash) { throw 'Verification source changed during execution' }
    }
    $manifest = $before | ForEach-Object { "$($_.Hash)  $($_.Path.Substring($repository.Length + 1).Replace('\', '/'))" }
    [IO.File]::WriteAllLines((Join-Path $fixtures 'source-sha256.txt'), $manifest)
    $report.Add('PASS: source hashes unchanged; no imported file writes; source-gone restart passed.')
    $report.Add('Actual JDK 17, Unix, instructor machine and symbolic-link creation remain unverified.')
    [IO.File]::WriteAllLines((Join-Path $fixtures 'summary.txt'), $report)
    $report | ForEach-Object { Write-Output $_ }
    Write-Output "Evidence: $fixtures"
} finally { Pop-Location }
