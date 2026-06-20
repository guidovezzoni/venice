#!/usr/bin/env bash
# Initialise SDLC symlinks for Claude Code.
# Works on Linux and macOS.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMMANDS_SRC="$PROJECT_ROOT/docs/sdlc/commands"
CLAUDE_DST="$PROJECT_ROOT/.claude/commands/sdlc"

mkdir -p "$CLAUDE_DST"

created=0
skipped=0
updated=0

link_file() {
    local src="$1"
    local target="$2"
    local rel_src
    rel_src="$(realpath --relative-to="$(dirname "$target")" "$src")"

    if [ -L "$target" ]; then
        local existing
        existing="$(readlink "$target")"
        if [ "$existing" = "$rel_src" ]; then
            echo "  skip  $target (already correct)"
            skipped=$((skipped + 1))
            return
        fi
        rm "$target"
        ln -s "$rel_src" "$target"
        echo "  update $target -> $rel_src"
        updated=$((updated + 1))
    else
        [ -e "$target" ] && rm "$target"
        ln -s "$rel_src" "$target"
        echo "  create $target -> $rel_src"
        created=$((created + 1))
    fi
}

echo "Linking SDLC for Claude Code..."
echo ""

# CLAUDE.md -> AGENTS.md
link_file "$PROJECT_ROOT/AGENTS.md" "$PROJECT_ROOT/CLAUDE.md"

# SDLC command symlinks
for file in "$COMMANDS_SRC"/*.md; do
    link_file "$file" "$CLAUDE_DST/$(basename "$file")"
done

echo ""
echo "Done: $created created, $updated updated, $skipped skipped."
