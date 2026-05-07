# Plan: Port handover Python pipeline into the repo

**Epic:** E2 (batch analysis pipeline)
**Design:** none — straightforward port with two known fixes (CLI flags, path conventions).

## Summary

Move the two handover Python scripts (`batch_process_zita.py`, `analyze_zita_results.py`) into the repo under `scripts/`, fix the CLI flag mismatches against Zita's actual interface, and make the defaults Linux-friendly. Add a `scripts/README.md` explaining usage.

**Decisions locked in:**
- Location: `scripts/` (sibling to `src/`). Already used by similar Java/Maven projects.
- The scripts go into the repo and are committed (unlike the handover materials in `temp/handover/`, which stay local).
- Fix CLI: handover doc shows `--rulesets` and `--format` but Zita's actual flags are `--rules` and `--renderer`. Inspect the original Python first — the doc may have drifted from the script's actual behavior.
- Default paths assume `python scripts/batch_process_zita.py` is run from repo root.
- Python 3.8+; no third-party dependencies (use only `subprocess`, `pathlib`, `json`, `csv`).

---

## Phase 1: Inspect originals before moving

### [ ] Task 1.1: Read both Python files end-to-end
- [ ] `temp/handover/Analysis Scripts/scripts/batch_process_zita.py` — confirm whether the script actually uses `--rulesets`/`--format` (handover doc may be wrong) or already uses Zita's real flags
- [ ] `temp/handover/Analysis Scripts/scripts/analyze_zita_results.py` — confirm CSV column expectations match PMD's `CSVRenderer` output

### [ ] Task 1.2: Run originals against the testbed AS-IS
- [ ] Set up a temp playground that matches the handover's expected layout (`ZITA/Zita/...`, `ZITA/scripts/...`, `ZITA/submissions/...`) using symlinks
- [ ] Capture original behavior so port can be verified as a no-op semantic change

## Phase 2: Port

### [ ] Task 2.1: Copy + edit
- [ ] `cp temp/handover/Analysis\ Scripts/scripts/*.py scripts/`
- [ ] Update default paths in `batch_process_zita.py`:
  - `zita_jar` default: `target/Zita.jar`
  - `rules` default: `src/main/resources/rulesets/rules.xml`
  - `submissions_dir` default: required positional (no sensible default)
  - `output_dir` default: `<submissions_dir>/batch-analysis`
- [ ] Replace any `\` PowerShell line-continuations or path separators in defaults with POSIX
- [ ] Confirm CLI flag invocations are `--rules` / `--renderer` (not `--rulesets` / `--format`)

### [ ] Task 2.2: Wire to `csv` renderer for the analyzer
- [ ] `analyze_zita_results.py` expects PMD CSV output; verify column names match what `--renderer csv` actually emits

## Phase 3: Documentation

### [ ] Task 3.1: `scripts/README.md`
- [ ] End-to-end usage: build → batch run → analyze
- [ ] Bash command examples (not PowerShell)
- [ ] Document the convention "submission directory name == .pde filename"
- [ ] Link to upstream report: "results land in `<output_dir>/comparison_report.md`"

### [ ] Task 3.2: Mention in repo README.md
- [ ] One-liner under "Usage": "For batch analysis across many submissions, see `scripts/README.md`"

## Phase 4: Verification

### [ ] Task 4.1: End-to-end smoke
- [ ] `python scripts/batch_process_zita.py temp/handover/ExamplePrograms /tmp/zita-batch-out`
- [ ] `python scripts/analyze_zita_results.py /tmp/zita-batch-out/csv`
- [ ] `comparison_report.md` matches the structure shown in `Zita_Handover.md` § 3.2

## Verification

- [ ] Both scripts run on Linux with no Python errors
- [ ] CLI flags align with Zita's actual interface
- [ ] `scripts/README.md` is sufficient for a new user to run the pipeline
- [ ] One commit per file move + one commit for the README
