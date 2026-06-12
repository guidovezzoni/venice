# Git Guidelines

## Commit Discipline
- CRITICAL: never ever commit directly, always ask confirmation if committing is deemed important
- After implementing a plan, always suggest a commit message summarising the operations performed in such plan. Prepend that suggestion with [Suggested Commit Message]
- **Amend means files, not commits**: When asked to "amend" something, always interpret it as
  amending file content. Never `git commit --amend` unless explicitly asked to amend a **commit**.
  When in doubt, ask for confirmation.

## Commit Message Format
1. **First line**: simple and concise summary of the change, prefixed with the issue reference number if available (check the branch name as it often starts with one)
2. **Body** (optional): extra detail if required, max 20 words

## File Operations
- Always use `git mv` instead of plain `mv` when renaming or moving tracked files, so git tracks the change

## Branch Verification
- Always verify the current branch by running `git branch --show-current` before acting on branch state. Do not trust cached or snapshot-based branch information — it may be stale.

## Branch Strategy
- `main` for stable releases
- Feature branches for new development
- Use descriptive branch names
