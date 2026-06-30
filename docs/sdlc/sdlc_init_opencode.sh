#!/usr/bin/env bash
# Initialise SDLC symlinks for OpenCode.
# Works on Linux and macOS.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMMANDS_SRC="$PROJECT_ROOT/docs/sdlc/commands"
OPENCODE_DST="$PROJECT_ROOT/.opencode/command"

mkdir -p "$OPENCODE_DST"

created=0
skipped=0
updated=0

link_file() {
    local src="$1"
    local basename_no_ext
    basename_no_ext="$(basename "$src" .md)"
    local target_name
    target_name="$(echo "$basename_no_ext" | tr '_' '-').md"
    local target="$OPENCODE_DST/$target_name"

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

echo "Linking SDLC for OpenCode..."
echo ""

# SDLC command symlinks (all commands including security review)
for file in "$COMMANDS_SRC"/*.md; do
    link_file "$file"
done

echo ""
echo "Done: $created created, $updated updated, $skipped skipped."
