#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET_ROOT="$(cd "$PROJECT_ROOT/../SDLC" 2>/dev/null && pwd)" || {
    echo "Error: ../SDLC/ directory not found relative to project root ($PROJECT_ROOT)"
    exit 1
}

SDLC_EXCLUDES=(
    "sync_sdlc.sh"
    ".claude"
)

build_rsync_excludes() {
    local args=()
    for item in "${SDLC_EXCLUDES[@]}"; do
        args+=(--exclude "$item")
    done
    printf '%s\n' "${args[@]}"
}

echo "Syncing SDLC framework: $PROJECT_ROOT -> $TARGET_ROOT"
echo

# 1. docs/guidelines/
echo "--- docs/guidelines/ ---"
mkdir -p "$TARGET_ROOT/docs/guidelines"
rsync -av --delete "$PROJECT_ROOT/docs/guidelines/" "$TARGET_ROOT/docs/guidelines/"
echo

# 2. docs/sdlc/ (with exclusions)
echo "--- docs/sdlc/ ---"
mkdir -p "$TARGET_ROOT/docs/sdlc"
rsync -av --delete $(build_rsync_excludes) "$PROJECT_ROOT/docs/sdlc/" "$TARGET_ROOT/docs/sdlc/"
echo

# 3. docs/sdlc/README.md -> repo root
echo "--- README.md (root copy) ---"
cp -v "$PROJECT_ROOT/docs/sdlc/README.md" "$TARGET_ROOT/README.md"
echo

# 4. AGENTS.md (top section only)
echo "--- AGENTS.md (framework section only) ---"
sed '/^## Project Overview/,$d' "$PROJECT_ROOT/AGENTS.md" > "$TARGET_ROOT/AGENTS.md"
printf '## Project Overview\n\n(TO BE COMPLETED)\n' >> "$TARGET_ROOT/AGENTS.md"
echo "  Updated $TARGET_ROOT/AGENTS.md"
echo

echo "Done."
