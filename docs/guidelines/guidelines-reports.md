# Report Guidelines

## Overview

Reports provide a human-readable, chronological record of work performed on a user story. Each workflow command appends its own section to the report, building up a complete history from story opening through to archival.

## Storage

All reports live in `docs/reports/`.

## File Format

- Reports are **HTML files** — not Markdown
- Self-contained: all CSS is embedded in a `<style>` block, no external stylesheets, scripts, fonts, or images
- Must render correctly when opened directly in a browser

## File Naming

Format: `{id}-{slug}.html`

Derived from the user story filename by stripping any `-WIP` or `-DONE` suffix and changing the extension to `.html`.

Examples:
- User story `1.2.5-edit-stop-WIP.md` → report `1.2.5-edit-stop.html`
- User story `1.2.3.1-consolidate-stop-usecases.md` → report `1.2.3.1-consolidate-stop-usecases.html`

One report per user story — never create multiple report files for the same story.

## Report Lifecycle

1. The **first command** that needs to write a section creates the full HTML skeleton with the report header and an empty `<main>` container
2. Each subsequent command **appends** its section before `</main>`
3. Commands **never modify or remove** existing sections — reports are append-only

## HTML Skeleton

Created when the report is first initialised. Replace `{id}`, `{title}`, `{branch}`, and `{date}` with actual values.

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Report: {id} — {title}</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.6;
      color: #1a1a1a;
      background: #fafafa;
      max-width: 960px;
      margin: 0 auto;
      padding: 2rem 1.5rem;
    }

    .report-header {
      border-bottom: 3px solid #2563eb;
      padding-bottom: 1rem;
      margin-bottom: 2rem;
    }
    .report-header h1 { font-size: 1.75rem; color: #1e293b; }
    .report-meta {
      display: flex; gap: 2rem; flex-wrap: wrap;
      margin-top: 0.5rem; font-size: 0.875rem; color: #64748b;
    }
    .report-meta span { display: inline-flex; align-items: center; gap: 0.25rem; }

    .report-section {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
      box-shadow: 0 1px 3px rgba(0,0,0,0.04);
    }
    .report-section header {
      display: flex; justify-content: space-between; align-items: baseline;
      border-bottom: 1px solid #e2e8f0;
      padding-bottom: 0.75rem; margin-bottom: 1rem;
    }
    .report-section header h2 { font-size: 1.25rem; color: #1e293b; }
    .report-section header time { font-size: 0.8rem; color: #94a3b8; white-space: nowrap; }
    .section-source { font-size: 0.75rem; color: #94a3b8; font-style: italic; margin-bottom: 0.75rem; }

    h3 { font-size: 1.05rem; color: #334155; margin: 1rem 0 0.5rem; }
    p { margin-bottom: 0.75rem; }
    ul, ol { margin: 0.5rem 0 0.75rem 1.5rem; }
    li { margin-bottom: 0.25rem; }
    code {
      background: #f1f5f9; padding: 0.15rem 0.35rem; border-radius: 3px;
      font-size: 0.875em;
      font-family: "SF Mono", "Fira Code", Menlo, Consolas, monospace;
    }

    table { width: 100%; border-collapse: collapse; margin: 0.75rem 0; font-size: 0.9rem; }
    th, td { text-align: left; padding: 0.5rem 0.75rem; border: 1px solid #e2e8f0; }
    th { background: #f8fafc; font-weight: 600; color: #475569; }

    .badge {
      display: inline-block; padding: 0.15rem 0.5rem;
      border-radius: 4px; font-size: 0.8rem; font-weight: 600;
    }
    .badge-pass { background: #dcfce7; color: #166534; }
    .badge-fail { background: #fee2e2; color: #991b1b; }
    .badge-warn { background: #fef9c3; color: #854d0e; }
    .badge-info { background: #dbeafe; color: #1e40af; }

    .outcome { margin-top: 1rem; padding: 0.75rem 1rem; border-radius: 6px; font-weight: 600; }
    .outcome-passed { background: #dcfce7; color: #166534; border-left: 4px solid #22c55e; }
    .outcome-failed { background: #fee2e2; color: #991b1b; border-left: 4px solid #ef4444; }
  </style>
</head>
<body>

  <div class="report-header">
    <h1>{id} — {title}</h1>
    <div class="report-meta">
      <span><strong>Story:</strong> {id}</span>
      <span><strong>Branch:</strong> {branch}</span>
      <span><strong>Created:</strong> {date}</span>
    </div>
  </div>

  <main id="report-sections">
  </main>

</body>
</html>
```

## Section Template

Each command appends a section using this structure. Replace placeholders with actual values.

```html
  <section class="report-section" data-source="{command-name}" data-timestamp="{ISO-8601}">
    <header>
      <h2>{Section Title}</h2>
      <time datetime="{ISO-8601}">{human-readable date}</time>
    </header>
    <p class="section-source">Source: {command-name}</p>
    <!-- Command-specific content goes here -->
  </section>
```

- `data-source`: the command that produced this section (e.g. `open_user_story`, `opsx:propose`, `apply_changes`, `verify_user_story`, `opsx:archive`)
- `data-timestamp`: ISO 8601 timestamp of when the section was added
- The inner content is entirely determined by the command — the guideline does not prescribe it

## Appending Sections

- The insertion point is the line containing `</main>` — new sections are inserted **immediately before** this line
- `</main>` must always appear on its own line to keep insertion deterministic
- If the report file does not exist yet, create the full HTML skeleton first, then insert the section
- If `docs/reports/` does not exist, create the directory

## Available CSS Classes

Commands may use these utility classes for consistent styling. Their use is optional.

| Class | Purpose |
|-------|---------|
| `.badge-pass` | Green badge for passed items |
| `.badge-fail` | Red badge for failed items |
| `.badge-warn` | Yellow badge for warnings |
| `.badge-info` | Blue badge for informational items |
| `.outcome-passed` | Green block for overall passed outcome |
| `.outcome-failed` | Red block for overall failed outcome |

## Legacy Reports

Existing `.md` reports in `docs/reports/` are legacy and are not migrated. New reports use HTML exclusively.
