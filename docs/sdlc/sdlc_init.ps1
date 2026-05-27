# Initialise SDLC command symlinks for Claude Code and Cursor.
# Requires Windows 10+ with Developer Mode enabled, or an elevated prompt.

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = (Resolve-Path "$ScriptDir\..\..").Path
$CommandsSrc = Join-Path $ProjectRoot "docs\sdlc\commands"
$ClaudeDst = Join-Path $ProjectRoot ".claude\commands\sdlc"
$CursorDst = Join-Path $ProjectRoot ".cursor\commands"

if (-not (Test-Path $ClaudeDst)) { New-Item -ItemType Directory -Path $ClaudeDst -Force | Out-Null }
if (-not (Test-Path $CursorDst)) { New-Item -ItemType Directory -Path $CursorDst -Force | Out-Null }

$created = 0
$skipped = 0
$updated = 0

function Link-Command {
    param([string]$Src, [string]$DstDir)

    $filename = Split-Path -Leaf $Src
    $target = Join-Path $DstDir $filename

    if (Test-Path $target) {
        $item = Get-Item $target -Force
        if ($item.LinkType -eq "SymbolicLink" -and $item.Target -eq $Src) {
            Write-Host "  skip  $target (already correct)"
            $script:skipped++
            return
        }
        Remove-Item $target -Force
        New-Item -ItemType SymbolicLink -Path $target -Target $Src | Out-Null
        Write-Host "  update $target -> $Src"
        $script:updated++
    }
    else {
        New-Item -ItemType SymbolicLink -Path $target -Target $Src | Out-Null
        Write-Host "  create $target -> $Src"
        $script:created++
    }
}

Write-Host "Linking SDLC commands..."
Write-Host ""

Get-ChildItem -Path $CommandsSrc -Filter "*.md" | ForEach-Object {
    if ($_.Name -eq "SDLC-README.md") { return }

    Link-Command -Src $_.FullName -DstDir $ClaudeDst
    Link-Command -Src $_.FullName -DstDir $CursorDst
}

Write-Host ""
Write-Host "Done: $created created, $updated updated, $skipped skipped."
