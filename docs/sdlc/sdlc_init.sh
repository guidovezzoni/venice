#!/usr/bin/env bash
# Initialise SDLC command symlinks for Claude Code and Cursor.
# Works on Linux and macOS.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMMANDS_SRC="$PROJECT_ROOT/docs/sdlc/commands"
CLAUDE_DST="$PROJECT_ROOT/.claude/commands/sdlc"
CURSOR_DST="$PROJECT_ROOT/.cursor/commands"

mkdir -p "$CLAUDE_DST"
mkdir -p "$CURSOR_DST"

created=0
skipped=0
updated=0

link_command() {
    local src="$1"
    local dst="$2"
    local filename
    filename="$(basename "$src")"
    local target="$dst/$filename"

    if [ -L "$target" ]; then
        local existing
        existing="$(readlink "$target")"
        if [ "$existing" = "$src" ]; then
            echo "  skip  $target (already correct)"
            skipped=$((skipped + 1))
            return
        fi
        rm "$target"
        ln -s "$src" "$target"
        echo "  update $target -> $src"
        updated=$((updated + 1))
    else
        [ -e "$target" ] && rm "$target"
        ln -s "$src" "$target"
        echo "  create $target -> $src"
        created=$((created + 1))
    fi
}

echo "Linking SDLC commands..."
echo ""

for file in "$COMMANDS_SRC"/*.md; do
    filename="$(basename "$file")"
    [ "$filename" = "SDLC-README.md" ] && continue

    link_command "$file" "$CLAUDE_DST"
    link_command "$file" "$CURSOR_DST"
done

echo ""
echo "Done: $created created, $updated updated, $skipped skipped."
