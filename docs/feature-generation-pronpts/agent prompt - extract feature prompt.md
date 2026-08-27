FEATURE = [FEATURE]

Referring to the FEATURE implemented in this project, act as a feature-extraction and prompt-generation specialist.

## Phase 1: Analysis
* Locate and read all implementation code for the feature, plus any related docs, ADRs, or comments explaining *why* decisions were made (not just what was built).
* Identify the core mechanism: what problem does this solve, and what's the minimal set of components/steps that make it work?
* Separate concerns explicitly:
    - **Language/framework-specific details** (syntax, specific libraries, this project's file structure) — to be discarded
    - **Portable architecture** (the pattern, the sequence of steps, the integration points, the config/secrets needed) — to be kept
* Note any external dependencies (CI provider, cloud service, API, infra) that the new project will also need, regardless of language.
* Flag anything ambiguous or assumption-laden in the current implementation that a different project/language might need to resolve differently.

## Phase 2: Generalisation
* Abstract the feature into a language-agnostic design: goal, inputs/outputs, sequence of steps, decision points, and integration touchpoints.
* Where the original implementation made an opinionated choice (a specific tool, a specific pattern), note it as "reference choice" rather than a hard requirement — the new prompt should let the target agent pick the equivalent for its stack.

## Phase 3: Generate the new prompt
Output a new prompt, written to be sent standalone to an agent in a different project. It must instruct that agent to:
1. Restate its understanding of the feature goal and ask clarifying questions about the target project's stack, constraints, and existing conventions.
2. Propose an implementation plan (steps, tools/libraries suited to the target language, files to be touched) and wait for explicit confirmation before writing code.
3. Implement the feature following the target project's existing conventions (naming, structure, error handling style).
4. Verify what's verifiable — tests, dry-runs, lint, manual checklist for anything that can't be automated — and report results.
5. Summarise what was ported as-is vs adapted for the new stack, and flag any portable-but-unresolved decisions from Phase 1.

## Output
Write the generated prompt to `./prompts/[feature-name]-port.md`, using this structure: Goal, Context to gather, Plan & confirm, Implementation, Verification, Summary.