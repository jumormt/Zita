# Zita Future Extension Points

This file tracks documented extension points and deferred features.
When implementing an extension, move it to a plan file and update PROGRESS.md.

---

## Deferred Features

### HasRandomDirectionChangePatternRule (from E1)

- **What:** Detect the `random(1) < 0.01` idiom for sporadic direction changes
- **Why deferred:** Not in the 4 implemented `.kt` files; only proposed in `new_proposed_rules.md`
- **Current behavior:** Pattern is undetected. Hits 6/9 assC AI submissions per handover evidence.
- **When to implement:** After E1 baseline is in and we want broader AI-codegen coverage
- **Effort estimate:** ~2 hours (single regex/AST rule following `HasRandomDirectionPatternRule` template)
- **Changes required:** new `.kt` under `src/main/kotlin/.../rules/`, `<rule>` entry in `rules.xml`

### HasFrameCountMagicNumberRule (from E1)

- **What:** Detect frame-based timing constants like `final int X = 180; // 3 seconds at 60 fps`
- **Why deferred:** Same as above — proposed only
- **Current behavior:** Undetected. Hits 5/9 per handover.
- **When to implement:** After E1 baseline. Lower priority — pattern overlaps with general magic-number anti-pattern rules in PMD.
- **Effort estimate:** ~3 hours (need to combine literal-value AST check with comment scan)

### HasExcessiveInlineDocumentationRule (from E1)

- **What:** Detect over-commenting of obvious code (e.g., `background(0); // Black background each frame`)
- **Why deferred:** Proposed only. Highest hit rate of the medium tier (8/9) but also highest false-positive risk for genuine human pedagogical comments.
- **When to implement:** Only after careful threshold tuning against the 74-sketch sample
- **Effort estimate:** ~1 day (heuristic design + tuning)

### HasWikipediaReferenceRule (from E1)

- **What:** Detect Wikipedia URLs as default inspiration source
- **Why deferred:** Proposed only. 4/9 hit rate. Trivial to implement but very narrow scope.
- **Effort estimate:** ~30 min

### HasEmptyMethodBodyRule (from E1)

- **What:** Detect empty method stubs like `void update() {}`
- **Why deferred:** Proposed only. 1/9 hit rate — weak signal on its own.
- **When to implement:** Probably never as a standalone rule; could be folded into a broader "stub method" rule
- **Effort estimate:** ~1 hour

### Batch analysis pipeline port (from E2)

- **What:** Port `temp/handover/Analysis Scripts/scripts/{batch_process_zita.py,analyze_zita_results.py}` to Linux + Zita's actual CLI flags (`--rules`/`--renderer`, not `--rulesets`/`--format`)
- **Why deferred:** E1 (rule integration) is the prerequisite — no point automating analysis until the new rules are in
- **Current behavior:** Python scripts ship in handover but reference PowerShell paths and use wrong CLI flags
- **When to implement:** After E1 lands. Decide first whether scripts live in repo (`scripts/`) or stay external.
- **Effort estimate:** ~1 day (path/CLI fixes + cross-platform testing)

---

## Known Limitations

### `processing-java` is required for full DoesItBuildRule coverage (from E0)

- **Issue:** `DoesItBuildRule` shells out to the `processing-java` binary (Processing IDE's CLI component). Without it, every submission gets a "failed to build" violation regardless of its real state.
- **Impact:** Renderers still produce useful output for all OTHER rules; only the build-validation rule is degraded.
- **Current mitigation:** None in repo. Users need to install Processing IDE and add `processing-java` to PATH.
- **If problematic:** Document the dependency in README and/or detect missing binary at startup with a clearer message than the current `Cannot run program "processing-java"` exception.

### No CI on `main` (from E0)

- **Issue:** `.github/workflows/merge-to-dev.yml` only triggers on PRs to `dev`, but `dev` does not exist on the upstream remote. Direct commits to `main` (which is the actual landing branch per recent history) are never built.
- **Impact:** Compile failures and ruleset-loading bugs ship to `main` undetected — this is exactly how the two bugs in E0 reached users.
- **Current mitigation:** None.
- **If problematic:** Add a `push: branches: [main]` or `pull_request: branches: [main]` trigger to the existing workflow. Effort: ~10 min.

### No automated tests (from E0)

- **Issue:** No `src/test/` directory, no JUnit/Kotest dependency, no Maven `<plugin>` for surefire/failsafe. Every rule change is validated by manually running the JAR against sample sketches.
- **Impact:** Refactors are risky; rule logic changes can silently regress on edge cases.
- **Current mitigation:** The 74 `.pde` files in the handover (`temp/handover/ExamplePrograms/`) plus the documented hit-rate baselines act as a manual regression suite.
- **If problematic:** Convert the handover sample set into a JUnit `@ParameterizedTest` driver that asserts violation counts per rule per sketch. Effort: ~2-3 days.
