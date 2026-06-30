#!/usr/bin/env bash
# Initialise SDLC symlinks for Codex CLI.
# Works on Linux and macOS.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMMANDS_SRC="$PROJECT_ROOT/docs/sdlc/commands"
CODEX_DST="$PROJECT_ROOT/.codex/skills"

mkdir -p "$CODEX_DST"

created=0
skipped=0
updated=0

link_skill() {
    local src="$1"
    local basename_no_ext
    basename_no_ext="$(basename "$src" .md)"
    local skill_dir_name
    skill_dir_name="$(echo "$basename_no_ext" | tr '_' '-')"
    local skill_dir="$CODEX_DST/$skill_dir_name"
    local target="$skill_dir/SKILL.md"

    mkdir -p "$skill_dir"

    local rel_src
    rel_src="$(realpath --relative-to="$skill_dir" "$src")"

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

echo "Linking SDLC for Codex CLI..."
echo ""

# SDLC command symlinks (all commands including security review)
for file in "$COMMANDS_SRC"/*.md; do
    link_skill "$file"
done

echo ""
echo "Done: $created created, $updated updated, $skipped skipped."
