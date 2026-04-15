# Process and Workflow Guidelines


## Development Workflow
1. **Before Coding**: Run `./gradlew clean` to ensure clean state
2. **During Development**: Use `./gradlew assembleDebug` for quick builds
3. **After applying changes**: Run `./gradlew check` to ensure code quality
4. **Testing**: Run `./gradlew test` for unit tests, `./gradlew connectedAndroidTest` for UI tests
5. **Final Build**: Use `./gradlew build` for complete verification

## git related
- CRITICAL: never ever commit directly, always ask confirmation if committing is deemed important
- After implementing a plan, always suggest a commit message summarising the operations performed in such plan. Prepend that suggestion with [Suggested Commit Message]
- Be descriptive but concise
- Reference relevant issue number if available, you can check the branch name as it oftens starts with an issue number

## Branch Strategy
- `main` for stable releases
- Feature branches for new development
- Use descriptive branch names
