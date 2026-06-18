# Initialise SDLC symlinks for Codex CLI.
# Requires Windows 10+ with Developer Mode enabled, or an elevated prompt.

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = (Resolve-Path "$ScriptDir\..\..").Path
$CommandsSrc = Join-Path $ProjectRoot "docs\sdlc\commands"
$CodexDst = Join-Path $ProjectRoot ".codex\skills"

if (-not (Test-Path $CodexDst)) { New-Item -ItemType Directory -Path $CodexDst -Force | Out-Null }

$created = 0
$skipped = 0
$updated = 0

function Link-Skill {
    param([string]$Src, [string]$SkillDir)

    $Target = Join-Path $SkillDir "SKILL.md"

    if (-not (Test-Path $SkillDir)) { New-Item -ItemType Directory -Path $SkillDir -Force | Out-Null }

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

Write-Host "Linking SDLC for Codex CLI..."
Write-Host ""

# SDLC command symlinks - each command becomes a directory with a SKILL.md symlink
Get-ChildItem -Path $CommandsSrc -Filter "*.md" | ForEach-Object {
    $baseName = $_.BaseName
    # Convert underscores to hyphens for kebab-case directory name
    $skillName = $baseName -replace '_', '-'
    $skillDir = Join-Path $CodexDst $skillName
    Link-Skill -Src $_.FullName -SkillDir $skillDir
}

Write-Host ""
Write-Host "Done: $created created, $updated updated, $skipped skipped."
