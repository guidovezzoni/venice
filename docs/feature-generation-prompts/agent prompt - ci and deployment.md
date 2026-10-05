# Port: Continuous Integration and Automated Deployment

A standalone prompt. Send it to an agent working in any project, in any language, on any platform.

---

## Goal

Set up **continuous integration** and **automated deployment** for this project: every change is
validated automatically, and a release reaches its distribution channel by pushing a tag rather than
by someone running commands on a laptop.

The problem being solved: release processes rot in two directions at once. They become **manual** — a
sequence of steps living in one person's memory, unreproducible when that person is away — and they
become **secret-entangled** — the validation pipeline needs signing keys and service accounts, so it
cannot run on a fork, cannot run for a contributor, and breaks whenever a credential rotates.

The four things that prevent this, and which this port exists to reproduce:

1. **Validation and release are separate pipelines with different secret requirements.** Validation
   runs on every change and needs as close to zero secrets as the stack allows. Release needs
   everything, and runs rarely and deliberately.
2. **The build degrades gracefully when secrets are absent.** A missing signing key produces an
   unsigned build, not a failure. This is what lets rule 1 hold, and it is the single most portable
   idea here.
3. **The release logic lives in a tool that also runs locally**, not in pipeline YAML. What CI runs on
   a tag is the same command a developer can run by hand to debug it.
4. **The release trigger is a tag, and the tag's grammar selects the channel.** No dropdown, no
   remembering which track, no separate config to keep in sync.

Everything below serves those four ends. Where a rule cannot be followed in this stack, the intent
above is what must survive.

**Before writing anything**, restate your understanding of this goal in your own words, then work
through *Context to gather*.

---

## Context to gather

Read this project first, then **ask the user about anything the code does not settle.** Do not guess,
and do not proceed to *Plan & confirm* with open questions.

### Discover from the codebase

- Language, build system, and how a production artefact is currently produced — the exact command, if
  one exists.
- **Every credential, key, or config file the build already reads**, and how it is supplied today.
  Look for gitignored config files, environment variables read at build time, and templates checked in
  alongside the real thing.
- Whether the build **already tolerates** a missing credential, or fails hard. This determines how
  much of the work is build-configuration change rather than pipeline authoring.
- The existing quality gates: test command, static analysis, linting, coverage threshold — and whether
  a single aggregate command runs them all.
- Any existing pipeline definitions, release scripts, or `Makefile`/task-runner targets.
- Where the version number lives, and whether anything currently derives it from git.
- Branching convention in use, and whether release branches exist.
- **The project's conventions**: file naming, script style, how existing automation is documented.

### Ask the user

- **Which forge and CI provider**, and whether hosted runners are acceptable or self-hosted ones are in
  play. *(Self-hosted materially changes the secret-handling rules below — see Implementation.)*
- **What the deployment target is** — an app store, a package registry, a container registry, a server,
  a static host — and whether an account, listing, or namespace already exists there.
- **Which release channels** exist on that target (production, beta, canary, pre-release …) and what
  the promotion path between them is.
- Whether **first publication has been done manually**. Most stores and registries reject an automated
  first upload into a namespace that does not exist yet.
- **Who holds the credentials**, whether they can create a service account or deploy token scoped to
  publishing only, and what the rotation expectation is.
- Whether releases should also be **published on the forge itself** (a GitHub/GitLab Release with
  attached artefacts and generated notes), or only to the distribution target.
- **Whether version numbering is manual or derived.** See the flag in *Summary* — this is the most
  common unresolved decision in the reference implementation.
- Whether the repo accepts **pull requests from forks**. If it does, secret availability on PR builds
  is a real constraint, not a hypothetical one.
- Whether **end-to-end / device / integration tests** should run in validation, or are deliberately
  excluded for cost and flakiness.

---

## Plan & confirm

Two gates. **Do not write pipeline or build-configuration code until both are approved.** They are
separate because they answer different questions: the first is about what the pipeline must guarantee,
the second about how.

### When this guidance and the project disagree

This prompt was written from one project's implementation — an Android app publishing to Google Play
via GitHub Actions and Fastlane. It **will** disagree with this project somewhere: a different forge,
a target with no concept of channels, a build tool with no notion of unsigned output.

**Non-trivial discrepancies are discussed with the user, not resolved unilaterally.** Neither side
automatically wins: this guidance is not authoritative over a project it has never seen, and a local
convention is not automatically right either — the point of raising it is that a human decides.

**Trivial — decide yourself, proceed, and record it in the *Summary*:**

- File naming and placement of pipeline definitions within the provider's expected directory.
- Which action, plugin, or marketplace step performs a mechanical job (checkout, toolchain setup,
  dependency caching).
- Job and step naming, ordering of independent steps, log-verbosity flags.
- Substituting the direct equivalent of a reference tool for this stack's ecosystem.

**Non-trivial — stop and discuss:**

- **Adding a new tool or runtime to the build** — a release-automation tool, a language runtime the
  project does not otherwise need.
- **Anything that creates, scopes, or stores a credential**, or changes who can trigger a release.
- **Relaxing a quality gate** to make the pipeline pass. Never do this silently; a gate that fails is
  information.
- **Changing how the version number is produced**, or making git state authoritative over it.
- **Publishing anywhere externally for the first time.** A first upload to a real store, registry, or
  public release page is not reversible in the way a code change is — it needs explicit sign-off, and
  a dry-run first if the target supports one.
- **An existing pipeline or release script that overlaps** with what this asks you to build. Do not
  build a parallel one; ask whether to extend, replace, or wrap it.
- Any structural rule that **cannot hold here** — no graceful degradation available in the build tool,
  no channel concept on the target, secrets unavailable to the validation pipeline at all.

**The test:** would a maintainer be surprised to find this decided without them? If it changes
credentials, what gets published, who can publish, or the quality bar, the answer is yes.

**Timing.** Surface discrepancies at **Gate 2**, batched into one round. If one only becomes apparent
during implementation, **stop and raise it then** — do not press on and mention it in the *Summary*.
When raising one, state what the guidance is trying to achieve, what this project does instead, the
options you see, and your recommendation.

### Gate 1 — the pipeline contract

Produce a short written contract and get it approved. It states, in a table per pipeline:

| For each pipeline | State |
|---|---|
| **Trigger** | What events start it, and what deliberately does not |
| **Secrets required** | The exact list — and for validation, justify every entry, because zero is the target |
| **Steps** | The ordered gates, and which are blocking |
| **Concurrency** | Whether a newer run cancels an older one |
| **Outputs** | What exists afterwards that did not before |

Alongside it:

- **The secret inventory** — one row per credential: what it is, where it comes from, how it is
  encoded for storage, what file or variable it becomes at runtime, and which pipeline needs it.
- **The trigger grammar** — the exact tag or branch patterns and what each maps to. Write out worked
  examples, including one that must be *rejected*.
- **What is deliberately excluded**, with the reason. Excluding device tests or coverage upload for
  cost is a legitimate decision; leaving it unrecorded is not.

### Gate 2 — the implementation plan

The files to create or modify, the tools to introduce, and the manual prerequisites the user must
complete before any of it can run end to end. Manual prerequisites are part of the plan, not an
afterthought — several of them have multi-day lead times.

---

## Implementation

### Two pipelines, split by secret requirement

Not by speed, and not by "CI vs CD" as a naming habit. The split exists because **the validation
pipeline's reliability is inversely proportional to the number of secrets it touches.** A secret that
is unavailable on a fork PR, expired, or newly rotated turns a green pipeline red for reasons that
have nothing to do with the change under review.

| Pipeline | Runs | Secrets | Purpose |
|---|---|---|---|
| **Validation** | Every push to the trunk and every PR targeting it | Ideally none | Compile, test, static analysis, coverage gate, produce both debug-equivalent and production-equivalent builds |
| **Release** | Manual dispatch, and tag pushes matching the release grammar | All of them | Build the signed/production artefact, publish it, record the release |

**Concurrency differs deliberately.** Validation cancels superseded runs on the same ref — only the
latest commit's result matters, and cancelling saves runner time. Release **never** cancels in
progress. A deploy interrupted mid-upload can leave the target in a state neither the pipeline nor the
operator can see.

**Build the production configuration in validation too**, even though it is thrown away. Production
builds enable optimisation, minification, tree-shaking, or dead-code elimination that development
builds do not, and those steps break independently of anything the test suite covers. Finding that out
on a tag push is finding out too late.

### Graceful degradation is a build-configuration change

This is the load-bearing idea, and it lives in the **build configuration, not the pipeline**.

The build reads each credential from a well-known location, and when that location is absent it
**takes a documented fallback path** rather than failing:

- A missing signing credential produces an **unsigned** production artefact. The build succeeds; the
  output simply cannot be published.
- A missing API key resolves to an **empty default**, so compilation and tests proceed.
- Where a third-party plugin *demands* a config file and would otherwise fail with an opaque internal
  error, **intercept it with your own check that fails with an actionable message** — one that names
  the file, says where a developer gets it, and says how CI supplies it. The reference implementation
  hooks the plugin's own task to do this.

Two consequences follow, and both are why this is worth the effort:

- **The validation pipeline needs no secrets**, so it works identically for maintainers, contributors,
  and forks.
- **A fresh clone builds and tests** without anyone hunting for credentials, which is the same
  property from the developer-onboarding side.

Check in a **template** for every credential file (`*.template`, `*.example`) showing the expected keys
with placeholder values, and gitignore the real thing. The template is the documentation.

### Secret handling

**Never commit a credential.** Store each one in the CI provider's secret store, and **materialise it
at runtime** into the file or environment variable the build already expects.

- **Binary credentials** (keystores, certificates, p12 files) are stored **base64-encoded** and decoded
  into the workspace as a pipeline step. Secret stores are text-only; this is the standard workaround.
- **Composite credentials** — a keystore plus its passwords and alias — are stored as separate secrets
  and **assembled into the config file** the build reads. Keep the pieces separate in storage so one
  can be rotated alone.
- **Reference generated files by absolute path.** A decoded keystore written to the workspace root and
  referenced relatively will not resolve once the build tool changes working directory. Use the
  provider's workspace-root variable.
- **Quote defensively when writing JSON or multi-line secrets to a file.** A naive shell echo breaks on
  quote characters inside the payload; prefer the provider's file-writing mechanism or a heredoc over
  string interpolation.

**On hosted runners**, plaintext credential files in the workspace are acceptable — the VM is destroyed
after the job. **On self-hosted runners this assumption is false**: files persist, and later jobs from
other repositories may read them. If this project uses self-hosted runners, that changes the design and
must be raised at Gate 2.

### Release logic in a tool, not in the pipeline

Put the build-and-publish sequence in a **release-automation tool with named, self-contained
tasks** — one per channel. The pipeline's job shrinks to: set up the environment, materialise the
secrets, invoke one named task.

Three reasons this is worth the extra dependency:

1. **A developer can run the exact same task locally** to debug a publishing failure, which is
   otherwise the hardest class of CI failure to diagnose — it only reproduces in the environment you
   cannot attach to.
2. **Publishing to a real store or registry is fiddly** — resumable uploads, metadata handling,
   staged rollouts, track promotion. Reimplementing that in shell is how it stays subtly broken.
3. **Channel logic is code, in one file**, rather than duplicated across pipeline branches.

*Reference choice:* Fastlane, for a mobile app. Pick the equivalent for this stack — a language-native
release plugin, a task-runner target, a publishing CLI. What matters is that it is invocable by name
from both CI and a laptop.

**Keep each channel's task explicit rather than parameterising one task by track.** The duplication is
a handful of lines, and it makes "what exactly happens for a production release?" answerable by
reading one block.

### The tag grammar selects the channel

Define a tag grammar where the **suffix identifies the channel**:

```
v1.2.3            → production / stable
v1.2.3-rc1        → open beta / pre-release
v1.2.3-alpha1     → closed alpha / internal
```

Push the tag; the pipeline reads it and selects the corresponding task. Keep a **manual dispatch
trigger with an explicit channel input** alongside it, for the first run and for recovery — but treat
tags as the normal path.

**Guard the guard.** In the reference implementation the tag path is protected by a check that the tag
is reachable from a `release/*` branch, so a tag pushed on a feature branch cannot reach production.
**That check does not apply to manual dispatch**, which means anyone able to run a workflow can deploy
from anywhere. Decide deliberately whether that is acceptable here, and if not, protect the manual path
too — with the provider's environment approvals, a branch restriction, or a repeat of the same check.

Prefer the CI provider's **native protection mechanisms** (protected environments, required reviewers,
tag protection rules) over a shell check where they exist. A shell guard is a fallback, and shell
string-matching against branch lists is easy to fool.

### Record the release on the forge

After a successful publish on a tag, create a **release entry on the forge** with the artefacts
attached and notes generated from the commit range. This gives you a record independent of the
distribution target — one you can still read after a store listing is changed, a version is rolled
back, or access to the publishing account lapses. Mark pre-release channels as pre-release.

Attach both the **distribution format** and the **directly installable format** where they differ.
Only the former is publishable, but the latter is what someone reaches for when reproducing a bug
against an exact release.

### Manual prerequisites

Some of these have long lead times and none can be automated. Identify them at Gate 2 and hand the
user an explicit checklist:

- The listing, namespace, or repository must **exist on the target**, and often the **first artefact
  must be uploaded by hand**. Automated first publication is commonly rejected.
- A **publishing service account or deploy token** must be created and granted the narrowest role that
  can publish. Permission propagation is not always immediate — in the reference implementation it took
  up to 48 hours.
- Every secret must be **added to the CI provider's secret store** before the first release run.
- Any **local prerequisites** for running the release tasks by hand.

---

## Verification

Run everything that can be run, and **report the actual results** — command output, pass/fail counts,
findings. Do not report a step as passing without having run it. Deployment verification is unusually
manual, so be honest about which items you executed and which you could not.

**Automated**

1. Every command the pipeline runs **succeeds locally first**, from a clean checkout. A pipeline is a
   bad place to discover a broken command.
2. **The graceful-degradation path works**: temporarily move every credential file aside, and confirm a
   full build and test run still succeeds and produces an unsigned/unpublishable artefact.
3. **The credential-absent error messages are actionable** where a hard failure is unavoidable —
   trigger them and read what they say.
4. The **validation pipeline passes on a real PR**, and passes with **no secrets configured**.
5. The **production build configuration compiles and runs** — the optimised path, not just the
   development one.
6. The **quality gates actually fail the pipeline** when violated. Introduce a temporary lint error and
   a temporary coverage drop and confirm each turns the run red. A gate nobody has seen fail may not be
   wired.

**Manual checklist** — the parts no test covers

7. **Run each release task locally**, against a test or internal channel if the target has one, before
   letting CI do it.
8. **Trigger the release pipeline manually into the least-consequential channel first.** Confirm the
   artefact arrives at the target, and that the version it reports is the one you expected.
9. **Test the tag grammar** end to end for each channel, and confirm a **tag that must be rejected is
   rejected** — a release tag on a non-release branch. Verifying the happy paths without verifying the
   guard means the guard is untested.
10. Confirm the **forge release entry** appears with the right artefacts attached and pre-release
    marked correctly.
11. **Inspect the pipeline logs for leaked secret values.** Confirm the provider's masking held, and
    that no decode, assembly, or debug step echoed a credential.
12. **Confirm nothing sensitive was committed** — check the ignore rules cover every credential file
    and every generated artefact, and check git history, not just the working tree.
13. **Verify the published artefact is correctly signed** and is the exact build the pipeline produced.
14. If the target has channels, confirm the **promotion path** works, or record who does it manually.

---

## Summary

Finish by reporting, in prose:

**Ported as-is** — which parts of the design transferred unchanged.

**Adapted** — what changed for this stack and why: the release tool substituted, how graceful
degradation was achieved in this build system, how secrets are materialised, and what protects the
release path. Split this into **decided by you** (the trivial discrepancies you settled alone, listed
so the user can object to any of them now) and **agreed with the user** (what was raised and how it was
resolved).

**Unresolved decisions** — flag every one of these that this project has not settled, rather than
letting it pass as decided. Each is genuinely open in the reference implementation:

- **Version numbering.** In the reference implementation the version lives hand-edited in the build
  configuration while the release is triggered by a tag, and **nothing checks that the two agree**. A
  tag can ship an artefact whose internal version says something else, and nothing errors. State
  plainly whether this project derives the version from the tag, validates the two against each other,
  or has the same silent gap.
- **Manual dispatch bypasses the branch guard.** Record who can trigger a release, from where, and
  whether that matches who is supposed to be able to.
- **Fork PRs and secrets.** If the validation pipeline touches any secret at all, it will behave
  differently for forks. Say whether the project accepts fork PRs and whether this was tested.
- **What validation deliberately excludes** — device tests, integration tests, end-to-end tests,
  security scanning, dependency auditing. Each exclusion is defensible; each should be written down
  with its reason and revisited when the cost calculus changes.
- **Credential rotation.** Nobody owns it until it expires. Record where each credential lives, its
  expiry if it has one, and who rotates it.
- **Runner assumptions.** The plaintext-credentials-in-workspace pattern is safe on ephemeral hosted
  runners and unsafe on persistent self-hosted ones. Say which this project uses, so a future migration
  does not silently invalidate the design.
- **Rollback.** Say what happens when a bad release ships — whether the target supports halting a
  rollout or reverting to a previous version, and whether anyone has tried it.
- **Anything the codebase left ambiguous** that a reader would otherwise assume was decided.

**What only humans can complete.** End with the manual prerequisite checklist and its current state —
which items are done, which are pending, and which have a lead time. The pipeline is not verified until
those are closed, and saying so is more useful than an optimistic green.
