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
    local skill_dir="$2"
    local target="$skill_dir/SKILL.md"

    mkdir -p "$skill_dir"

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

echo "Linking SDLC for Codex CLI..."
echo ""

# SDLC command symlinks — each command becomes a directory with a SKILL.md symlink
for file in "$COMMANDS_SRC"/*.md; do
    basename="$(basename "$file" .md)"
    # Convert underscores to hyphens for kebab-case directory name
    skill_name="$(echo "$basename" | tr '_' '-')"
    link_skill "$file" "$CODEX_DST/$skill_name"
done

echo ""
echo "Done: $created created, $updated updated, $skipped skipped."
