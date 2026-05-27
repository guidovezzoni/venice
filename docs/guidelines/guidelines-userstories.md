# User Story Management Guidelines

This file is the single source of truth for how user stories are managed.
If the tooling changes (e.g. from Markdown files to Jira), update only this file.

## Storage

User stories are stored as Markdown files in `docs/userstories/`.

## Index

The file `docs/userstories/index.md` is the master index of all user stories, organised by Epic and Feature. Each entry links to the corresponding user story file. The index must be kept in sync with filename changes caused by state transitions.

## File Naming

```
{id}-{slug}[-STATE].md
```

- **id**: hierarchical identifier (e.g. `1.2.4`)
- **slug**: kebab-case short description (e.g. `reorder-stops`)
- **STATE**: optional lifecycle suffix (`WIP` or `DONE`)

## Lifecycle

A user story progresses through three states, reflected in its filename:

| State       | Filename pattern        | Example                        |
|-------------|-------------------------|--------------------------------|
| New         | `{id}-{slug}.md`        | `1.2.5-edit-stop.md`          |
| In Progress | `{id}-{slug}-WIP.md`    | `1.2.5-edit-stop-WIP.md`      |
| Done        | `{id}-{slug}-DONE.md`   | `1.2.5-edit-stop-DONE.md`     |

### Next User Story

When no specific story is specified, the next story to open is the first one in the index that is in the **New** state (i.e. not `-WIP` or `-DONE`).

### Opening a User Story

Rename the file by appending `-WIP` before the `.md` extension.

**Precondition:** The file must be in the **New** state. It cannot already be In Progress (`-WIP`) or Done (`-DONE`).

```
1.2.5-edit-stop.md  -->  1.2.5-edit-stop-WIP.md
```

### Closing a User Story

Rename the file by replacing the `-WIP` suffix with `-DONE` before the `.md` extension.

**Precondition:** The file must be in the **In Progress** state (`-WIP`). It cannot be New or already Done (`-DONE`).

```
1.2.5-edit-stop-WIP.md  -->  1.2.5-edit-stop-DONE.md
```

### Renaming

File renames must use `git mv` so the change is tracked in version control.

### Index Update

After any state transition, update the link in `docs/userstories/index.md` to point to the new filename.
