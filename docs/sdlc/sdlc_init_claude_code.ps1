# Initialise SDLC symlinks for Claude Code.
# Requires Windows 10+ with Developer Mode enabled, or an elevated prompt.

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = (Resolve-Path "$ScriptDir\..\..").Path
$CommandsSrc = Join-Path $ProjectRoot "docs\sdlc\commands"
$ClaudeDst = Join-Path $ProjectRoot ".claude\commands\sdlc"

if (-not (Test-Path $ClaudeDst)) { New-Item -ItemType Directory -Path $ClaudeDst -Force | Out-Null }

$created = 0
$skipped = 0
$updated = 0

function Link-File {
    param([string]$Src, [string]$Target)

    if (Test-Path $Target) {
        $item = Get-Item $Target -Force
        if ($item.LinkType -eq "SymbolicLink" -and $item.Target -eq $Src) {
            Write-Host "  skip  $Target (already correct)"
            $script:skipped++
            return
        }
        Remove-Item $Target -Force
        New-Item -ItemType SymbolicLink -Path $Target -Target $Src | Out-Null
        Write-Host "  update $Target -> $Src"
        $script:updated++
    }
    else {
        New-Item -ItemType SymbolicLink -Path $Target -Target $Src | Out-Null
        Write-Host "  create $Target -> $Src"
        $script:created++
    }
}

Write-Host "Linking SDLC for Claude Code..."
Write-Host ""

# CLAUDE.md -> AGENTS.md
Link-File -Src (Join-Path $ProjectRoot "AGENTS.md") -Target (Join-Path $ProjectRoot "CLAUDE.md")

# SDLC command symlinks
Get-ChildItem -Path $CommandsSrc -Filter "*.md" | ForEach-Object {
    Link-File -Src $_.FullName -Target (Join-Path $ClaudeDst $_.Name)
}

Write-Host ""
Write-Host "Done: $created created, $updated updated, $skipped skipped."
