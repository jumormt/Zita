# Plan: LDD scaffolding for Zita

**Epic:** E0 (repository hygiene)
**Design:** none — direct execution from the `/ldd-init` slash command.

## Summary

Bootstrap the Living Development Document methodology in this repository so that subsequent sessions on Zita (especially the AI-detection rule integration in E1) can be picked up cleanly without re-deriving context. Files are scoped to the `research/handover-2026-05-07` branch for now and are NOT auto-committed; the user can decide if/when to land them on `main`.

**Decisions locked in:**
- LDD lives under `docs/` per the skill convention.
- The CLAUDE.md session-workflow block goes into the repo's `CLAUDE.md`, which is in `.git/info/exclude` — so it stays local to this machine and never reaches upstream.
- The "first plan" is this scaffolding record. The next plan (handover rule integration) will be `2026-05-07-02-…` or a later date if work resumes on a different day.

---

## Phase 1: Files

### [x] Task 1.1: Create `docs/` skeleton
- [x] `mkdir -p docs/plans`
- [x] No `docs/designs/`, `docs/bugs/`, `docs/debug/`, `docs/summaries/`, `docs/specs/` yet — created on demand per LDD policy.

### [x] Task 1.2: `docs/PROGRESS.md`
- [x] Populate with real state, not template placeholders (current epic = E1, two prior bugfix PRs noted under Known Issues, today's session log filled out)

### [x] Task 1.3: `docs/plans/2026-05-07-01-scaffolding.md` (this file)
- [x] Self-document the bootstrap

### [x] Task 1.4: `docs/FUTURE.md`
- [x] Capture the 5 unimplemented proposed rules from `temp/handover/Analysis Scripts/scripts/new_proposed_rules.md` and the analysis-pipeline port as deferred items.

### [x] Task 1.5: CLAUDE.md LDD block
- [x] Append `references/template-claude-md-block.md` content to repo `CLAUDE.md`. Does not commit (CLAUDE.md is in `.git/info/exclude`).

## Verification

- [x] `docs/PROGRESS.md`, `docs/FUTURE.md`, `docs/plans/2026-05-07-01-scaffolding.md` exist
- [x] `git status` shows the three docs files as untracked (expected — user will decide if/when to commit)
- [x] CLAUDE.md contains the "Session Workflow" block
- [x] Plans Index in PROGRESS.md references this plan
