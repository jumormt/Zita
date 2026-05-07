# Plan: Regression baseline against 74-sketch handover testbed

**Epic:** E1 (AI-detection rule integration)
**Design:** none — empirical comparison.

## Summary

Run Zita over all 74 `.pde` projects in `temp/handover/ExamplePrograms/` twice — once with the pre-integration JAR (no new rules), once with the post-integration JAR (all 4 new rules). Compare violation tallies per rule per submission to (a) confirm the new rules' hit rates match the handover baseline and (b) detect any regressions in existing rules' counts.

**Decisions locked in:**
- Use the JSON renderer for machine-readable output (PMD's stock JSON format includes file/line/rule).
- Save both JARs side-by-side: `target/Zita.jar` (current) and `target/Zita-baseline.jar` (pre-integration snapshot from before plan 02 was executed).
- Output goes under `temp/regression-2026-05-07/` (local-only; under `.git/info/exclude` via `temp/`).

---

## Phase 1: Capture baseline

### [ ] Task 1.1: Snapshot the pre-integration JAR
- [ ] **Must run BEFORE plan 02 is executed.** Save current `target/Zita.jar` (the one built with both fixes but without the 4 new rules) as `target/Zita-baseline.jar`
- [ ] If plan 02 has already been executed, rebuild from `HEAD~N` to recreate the baseline

### [ ] Task 1.2: Discover all sample sketches
- [ ] Each Processing project is a *directory* containing one or more `.pde` files. Find all qualifying directories under `temp/handover/ExamplePrograms/`
- [ ] A directory qualifies if it contains at least one `.pde` file. Exclude `Archive/` per handover convention? — verify by reading `Zita_Handover.md` § 2 ("folder name must match .pde filename")
- [ ] Expected count: ~44 (per `analysis_report.md` summary) — sanity check

## Phase 2: Run baseline + post-integration sweeps

### [ ] Task 2.1: Write a small bash driver
- [ ] `temp/regression-2026-05-07/run.sh` — for each sample dir, invoke both JARs with `--renderer json`, save output to `baseline/<sample>.json` and `post/<sample>.json`
- [ ] Suppress per-invocation noise; log only failures
- [ ] Use the patched `temp/rules_test.xml`? No — by this point both upstream fixes are simulated-merged on this branch, so the shipped `src/main/resources/rulesets/rules.xml` works

### [ ] Task 2.2: Execute the sweep
- [ ] Run; capture wall-clock time
- [ ] Inspect any non-zero exits (likely `DoesItBuildRule` issues — `processing-java` not installed; should be a violation, not an exit error)

## Phase 3: Diff and analyze

### [ ] Task 3.1: Tally per rule per submission
- [ ] Python one-liner (jq or pure python): for each submission, count violations per `rule` field
- [ ] Produce `baseline_counts.csv` and `post_counts.csv`

### [ ] Task 3.2: Cross-check against handover baseline
- [ ] Per handover `Zita_Handover.md` § 6:
  - HasPlaceholderAuthorRule: 22 hits across 44 submissions
  - HasDecorativeSectionCommentsRule: 27
  - HasCitationCommentsRule: 7 (Gemini only)
  - HasRandomDirectionPatternRule: 12
- [ ] Document any deviation from these numbers in PROGRESS.md Notes

### [ ] Task 3.3: False-positive scan
- [ ] For each new rule, manually inspect 2-3 hits from human-written sketches (`temp/handover/ExamplePrograms/AssessmentB/Hopper/`, `FrogHopper/`, `Persona 1/AssessmentA/`) to confirm they don't fire on legitimate human code
- [ ] For each existing rule, verify count is identical between baseline and post — no regressions from the new rules' AST visitors interfering

## Verification

- [ ] Both JARs exist and differ in class count by exactly 4
- [ ] Both sweeps produce JSON for every sample
- [ ] Per-rule tally matches handover baselines within ±2 hits per rule (slack for sample-set discovery edge cases)
- [ ] No existing-rule count regression > 1
- [ ] Findings written into PROGRESS.md and a new `docs/summaries/2026-05-07-handover-integration.md`
