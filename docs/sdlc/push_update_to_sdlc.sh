#!/usr/bin/env bash
# Syncs SDLC framework files from this project to ~/dev/personal/SDLC.
#
# Sync behaviour (rsync --delete):
#   - Files added locally are copied to SDLC.
#   - Files removed locally are deleted from SDLC.
#   - Files added/removed in SDLC are overwritten/restored from local.
#
# Exclusions (never pushed to SDLC):
#   - push_update_to_sdlc.sh / get_update_from_sdlc.sh  — project-local sync scripts
#   - .claude                                             — project-local Claude config
#
# AGENTS.md: only the framework section (above ## Project Overview) is pushed;
#   a placeholder Project Overview is written in its place.
#
# See get_update_from_sdlc.sh for the reverse operation.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET_ROOT="$HOME/dev/personal/SDLC"

if [[ ! -d "$TARGET_ROOT" ]]; then
    echo "Error: target directory not found: $TARGET_ROOT"
    exit 1
fi

SDLC_EXCLUDES=(
    "push_update_to_sdlc.sh"
    "get_update_from_sdlc.sh"
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
