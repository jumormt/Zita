# Zita Development Progress

## Current Epic
**E1: AI-detection rule integration** — incorporate the 4 implemented + 5 proposed AI-codegen detection rules from the 2026-05-07 handover into Zita's main ruleset, so submissions across COMP1000 cohorts can be automatically screened.

## Epics
- [ ] E0: Repository hygiene — fix shipped-broken state on `main` (KDoc typo + missing `category` PropertyDescriptors). Two upstream PRs open.
- [ ] E1: AI-detection rule integration — port handover rules into `src/main/kotlin/.../rules/`, wire into `rules.xml`, regression-test against the 74 sample sketches.
- [ ] E2: Batch analysis pipeline — port `batch_process_zita.py` / `analyze_zita_results.py` to Linux + Zita's actual CLI, decide where they live in the repo.

## Plans Index (active/recent — see `SESSION-LOG-ARCHIVE.md` once it exists)
| Date | Plan | Epic | Status | Notes |
|------|------|------|--------|-------|
| 2026-05-07 | 01-scaffolding | E0 | done | LDD bootstrap: `docs/PROGRESS.md`, `docs/plans/`, `docs/FUTURE.md`, CLAUDE.md block (local only — CLAUDE.md is in `.git/info/exclude`). |
| 2026-05-07 | 02-handover-rule-integration | E1 | done | Commit `08b7090`. 4 rules from handover into `src/main/kotlin/.../rules/`, registered in `rules.xml`. Hit rates within ±1 of handover baselines. Summary: `docs/summaries/2026-05-07-handover-integration.md`. |
| 2026-05-07 | 03-regression-baseline | E1 | done | 46-sample sweep (`temp/regression-2026-05-07/`, local-only). Zero regression in existing rules. Per-rule comparison documented in summary. |
| 2026-05-07 | 04-remaining-proposed-rules | E1 | done | Commit `590cd90`. 5 new rules — Wikipedia/RandomDirectionChange/FrameCount/ExcessiveInlineDoc/EmptyMethodBody. All firing on testbed. `HasExcessiveInlineDocumentationRule` simplified to single-signal version (ratio signal deferred). |
| 2026-05-07 | 05-port-batch-pipeline | E2 | done | Commit `871c852`. `scripts/{batch_process_zita,analyze_zita_results}.py` + README. Smoke-tested 9/9 successful on AssessmentB Persona 2. |
| 2026-05-07 | 06-dockerize | infra | done | Multi-stage `Dockerfile` (299 MB runtime), `.dockerignore`, `.devcontainer/devcontainer.json`, `docs/DOCKER.md`. Batch script also reads `ZITA_JAR`/`ZITA_RULES` env vars (set by image). 1/1 + 9/9 batch smoke pass via container. |

## Next Steps
<!-- THE MOST IMPORTANT SECTION. This is what the next session reads first. Be specific. -->
- **No active in-flight plan.** All four user-requested tasks complete. Branch `research/handover-2026-05-07` has 3 commits on top of the two simulated upstream-PR merges.
- **Decision point for next session:** open an upstream PR for the 9 rules + scripts? Cannot open cleanly until upstream PRs #10 and #11 (the prerequisite bug fixes) merge — otherwise this PR's diff would include the simulation merge commits. Options:
  1. Wait for upstream #10 + #11 to land, then `git fetch upstream && git rebase upstream/main` on research branch, then open one combined PR or split into rules/scripts PRs.
  2. Open the rules/scripts PRs now against `Addzyyy/Zita:main` and let the maintainer reorder; simulation merges will be visible but harmless.
  3. Hold the work locally until you decide which rules belong upstream vs internal.
- **E0 follow-up unchanged:** upstream PRs `Addzyyy/Zita#10` (KDoc typo) and `#11` (category descriptors) still OPEN, awaiting upstream review.
- **Possible follow-up plans (not started):**
  - `07-junit-regression-harness`: convert the Bash regression sweep into a real test suite asserting per-rule hit counts. Would prevent the kind of regressions Plan 03 caught manually.
  - `08-tune-excessive-inline-doc`: empirically tune `narrationThreshold` after observing real-cohort false-positive rate.
  - `09-ci-on-main`: extend `merge-to-dev.yml` (or add a sibling workflow) to also build on `pull_request: branches: [main]` so direct-to-main typos can no longer slip past. Could now validate via `docker build .` for free.

## Known Issues
<!-- Persistent bugs, limitations, or risks that span sessions -->
- `src/main/resources/rulesets/rules.xml` ships with `<property name="category"/>` on `UsefulEventHandlerRule` and `HasClassWithConstructorRule` but the Kotlin classes don't define the descriptor — every renderer crashes at PMDRunner construction. Fixed locally on branch `fix/rules-xml-category-property-descriptors` (upstream PR #11).
- `DoesItBuildRule.kt:13` ships with `*/**` instead of `/**` — Kotlin compile fails on a clean checkout. Fixed locally on branch `fix/doesitbuildrule-kdoc-typo` (upstream PR #10).
- CI workflow `merge-to-dev.yml` only triggers on PRs to a `dev` branch that does not exist on origin — no automated build verification ever runs on `main`. Both bugs above made it through because of this.
- `processing-java` CLI is not installed locally. `DoesItBuildRule` falls back to a "failed to build" violation per submission instead of a real PASS/FAIL. Not blocking other rule analysis.
- `HasExcessiveInlineDocumentationRule` ships with single-signal heuristic only (narration count). Comment-to-code ratio signal deferred — would need source-text access that PMD doesn't expose for `ReaderDataSource` inputs (Zita's synthesis path).

## Known Issues
- `src/main/resources/rulesets/rules.xml` ships with `<property name="category"/>` on `UsefulEventHandlerRule` and `HasClassWithConstructorRule` but the Kotlin classes don't define the descriptor — every renderer crashes at PMDRunner construction. Fixed locally on branch `fix/rules-xml-category-property-descriptors` (upstream PR #11).
- `DoesItBuildRule.kt:13` ships with `*/**` instead of `/**` — Kotlin compile fails on a clean checkout. Fixed locally on branch `fix/doesitbuildrule-kdoc-typo` (upstream PR #10).
- CI workflow `merge-to-dev.yml` only triggers on PRs to a `dev` branch that does not exist on origin — no automated build verification ever runs on `main`. Both bugs above made it through because of this.
- `processing-java` CLI is not installed locally. `DoesItBuildRule` falls back to a "failed to build" violation per submission instead of a real PASS/FAIL. Not blocking other rule analysis.

## Key Decisions
| Category | Decision |
|----------|----------|
| Build | Maven 3.9.9 (portable install at `~/.local/tools/apache-maven-3.9.9/`); Java 11 release target; no `mvnw` wrapper in repo |
| Workflow | Cross-repo PRs go to `Addzyyy/Zita` (upstream); fork lives at `jumormt/Zita`. Co-Authored-By trailer disabled globally. |
| Local-only files | `CLAUDE.md`, `.claude/`, `temp/`, and `target/Zita.jar` are in `.git/info/exclude`. Handover materials live under `temp/handover/` and never enter git. |
| Testing convention | No test suite exists in the repo. Rule verification is empirical: run the JAR against `temp/handover/ExamplePrograms/*.pde` and compare violation counts to handover baselines. |

## Session Log
<!-- Append at the END. Move entries older than ~10 to SESSION-LOG-ARCHIVE.md. -->

### 2026-05-07 (session 1)
- **Focus:** Repo audit + LDD bootstrap
- **Completed:**
  - Authored `CLAUDE.md` for the repo (local only)
  - Discovered + fixed two shipped bugs: KDoc typo in `DoesItBuildRule.kt`, missing `category` `PropertyDescriptor` on two rules
  - Opened upstream PRs `Addzyyy/Zita#10` and `#11`; opened intra-fork PR `jumormt/Zita#1`
  - Built the project from scratch (`mvn -B clean package` → 8.3 MB shaded jar)
  - Verified all renderers (`zita`, `student`, `json`) end-to-end against shipped `rules.xml` (post-fix)
  - Created `research/handover-2026-05-07` branch with both fixes locally merged; extracted 109-file handover zip into `temp/handover/`
  - LDD scaffolded
- **Tests:** No test suite. Manual end-to-end verification only.
- **Files:** `DoesItBuildRule.kt` (1 char), `HasUsefulEventHandlerRule.kt` (+15), `HasClassWithConstructorRule.kt` (+15), `docs/PROGRESS.md`, `docs/plans/2026-05-07-01-scaffolding.md`, `docs/FUTURE.md` (new)
- **Branches:** local `main` clean; `research/handover-2026-05-07` HEAD = `fd19bd9` (pre-LDD scaffold)

### 2026-05-07 (session 2)
- **Focus:** Plans 02-05 — execute the four user-requested handover-integration tasks
- **Completed:** All 4 plans done. Branch HEAD now at `590cd90`.
  - Plan 02: 4 handover rules ported into `src/main/kotlin/.../rules/`, registered in `rules.xml` (commit `08b7090`). Per-rule hits: 23/22, 28/27, 8/7, 12/12 — all within ±1 of handover baselines.
  - Plan 03: 46-sample regression sweep in `temp/regression-2026-05-07/` (local-only). Zero regression in any existing rule.
  - Plan 04: 5 new rules from `new_proposed_rules.md` implemented (commit `590cd90`). All firing on testbed: Wikipedia 21, RandomDirectionChange 13, FrameCount 11, ExcessiveInlineDoc 12, EmptyMethodBody 1.
  - Plan 05: Python pipeline ported into `scripts/` with README (commit `871c852`). 9/9 successful smoke against AssessmentB Persona 2.
- **Tests:** Manual sweep 46 samples × 2 JARs = 92 invocations, all passed.
- **Files:** 9 new `.kt` files, `rules.xml` (+9 rule entries), `scripts/{batch_process_zita,analyze_zita_results}.py + README.md`, 5 plan files + 1 summary file under `docs/`.
- **Blockers:** Upstream PRs #10 and #11 must merge before this work can be cleanly PR'd to upstream (otherwise diff includes simulation merges).
