# Initialise SDLC symlinks for OpenCode.
# Requires Windows 10+ with Developer Mode enabled, or an elevated prompt.

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = (Resolve-Path "$ScriptDir\..\..").Path
$CommandsSrc = Join-Path $ProjectRoot "docs\sdlc\commands"
$OpenCodeDst = Join-Path $ProjectRoot ".opencode\command"

if (-not (Test-Path $OpenCodeDst)) { New-Item -ItemType Directory -Path $OpenCodeDst -Force | Out-Null }

$created = 0
$skipped = 0
$updated = 0

function Link-File {
    param([string]$Src)

    $basenameNoExt = [System.IO.Path]::GetFileNameWithoutExtension($Src)
    $targetName = ($basenameNoExt -replace '_', '-') + ".md"
    $target = Join-Path $OpenCodeDst $targetName

    $targetDir = Split-Path -Parent $target
    Push-Location $targetDir
    $relSrc = Resolve-Path -Relative $Src
    Pop-Location

    if (Test-Path $target) {
        $item = Get-Item $target -Force
        if ($item.LinkType -eq "SymbolicLink" -and $item.Target -eq $relSrc) {
            Write-Host "  skip  $target (already correct)"
            $script:skipped++
            return
        }
        Remove-Item $target -Force
        New-Item -ItemType SymbolicLink -Path $target -Target $relSrc | Out-Null
        Write-Host "  update $target -> $relSrc"
        $script:updated++
    }
    else {
        New-Item -ItemType SymbolicLink -Path $target -Target $relSrc | Out-Null
        Write-Host "  create $target -> $relSrc"
        $script:created++
    }
}

Write-Host "Linking SDLC for OpenCode..."
Write-Host ""

# SDLC command symlinks (all commands including security review)
Get-ChildItem -Path $CommandsSrc -Filter "*.md" | ForEach-Object {
    Link-File -Src $_.FullName
}

Write-Host ""
Write-Host "Done: $created created, $updated updated, $skipped skipped."
