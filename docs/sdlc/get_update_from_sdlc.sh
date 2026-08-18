#!/usr/bin/env bash
# Syncs SDLC framework files from ~/dev/personal/SDLC into this project.
#
# Sync behaviour (rsync --delete):
#   - Files added in SDLC are copied locally.
#   - Files removed in SDLC are deleted locally.
#   - Files added/removed locally are overwritten/restored from SDLC.
#
# Exclusions (never overwritten or deleted locally):
#   - push_update_to_sdlc.sh / get_update_from_sdlc.sh  — project-local sync scripts
#   - .claude                                             — project-local Claude config
#
# AGENTS.md: only the framework section (above ## Project Overview) is pulled;
#   the local ## Project Overview and everything below it is preserved unchanged.
#
# The SDLC root README.md is intentionally ignored (it is project-specific).
#
# See push_update_to_sdlc.sh for the reverse operation.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SOURCE_ROOT="$HOME/dev/personal/SDLC"

if [[ ! -d "$SOURCE_ROOT" ]]; then
    echo "Error: source directory not found: $SOURCE_ROOT"
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

echo "Syncing SDLC framework: $SOURCE_ROOT -> $PROJECT_ROOT"
echo

# 1. docs/guidelines/
echo "--- docs/guidelines/ ---"
mkdir -p "$PROJECT_ROOT/docs/guidelines"
rsync -av --delete "$SOURCE_ROOT/docs/guidelines/" "$PROJECT_ROOT/docs/guidelines/"
echo

# 2. docs/sdlc/ (with exclusions)
echo "--- docs/sdlc/ ---"
mkdir -p "$PROJECT_ROOT/docs/sdlc"
rsync -av --delete $(build_rsync_excludes) "$SOURCE_ROOT/docs/sdlc/" "$PROJECT_ROOT/docs/sdlc/"
echo

# 3. AGENTS.md (framework section only — keep local Project Overview intact)
echo "--- AGENTS.md (framework section only) ---"
SDLC_AGENTS="$SOURCE_ROOT/AGENTS.md"
LOCAL_AGENTS="$PROJECT_ROOT/AGENTS.md"

TEMP=$(mktemp)
trap 'rm -f "$TEMP"' EXIT

# Framework section from SDLC (everything before ## Project Overview)
sed '/^## Project Overview/,$d' "$SDLC_AGENTS" > "$TEMP"

# Project section from local (## Project Overview and everything after)
sed -n '/^## Project Overview/,$p' "$LOCAL_AGENTS" >> "$TEMP"

mv "$TEMP" "$LOCAL_AGENTS"
echo "  Updated $LOCAL_AGENTS"
echo

echo "Done."
