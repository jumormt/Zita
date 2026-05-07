# Summary: 2026-05-07 handover rule integration + pipeline port

**Plans covered:** 02 (handover rule integration), 03 (regression baseline), 04 (5 remaining proposed rules), 05 (Python pipeline port)
**Tags:** handover, ai-detection, rules, pmd, regression-testing, python

## Outcome

All four planned tasks shipped on the `research/handover-2026-05-07` branch as 3 commits:

- `08b7090` — 4 handover rules (`HasPlaceholderAuthorRule`, `HasDecorativeSectionCommentsRule`, `HasCitationCommentsRule`, `HasRandomDirectionPatternRule`)
- `871c852` — `scripts/batch_process_zita.py` + `scripts/analyze_zita_results.py` + README
- `590cd90` — 5 newly-implemented rules (`HasWikipediaReferenceRule`, `HasRandomDirectionChangePatternRule`, `HasFrameCountMagicNumberRule`, `HasExcessiveInlineDocumentationRule`, `HasEmptyMethodBodyRule`)

Final per-rule hit counts on the 46-sketch testbed (vs handover-documented expectations):

| Rule | Hits | Expected | Notes |
|------|-----:|---------:|-------|
| HasPlaceholderAuthorRule | 23 | 22 | ±1 |
| HasDecorativeSectionCommentsRule | 28 | 27 | ±1 |
| HasCitationCommentsRule | 8 | 7 | ±1 |
| HasRandomDirectionPatternRule | 12 | 12 | exact |
| HasWikipediaReferenceRule | 21 | 4 of 9 assC AI | broader catch — includes assA/B too |
| HasRandomDirectionChangePatternRule | 13 | 6 of 9 assC AI | broader catch |
| HasFrameCountMagicNumberRule | 11 | 5 of 9 assC AI | broader catch |
| HasExcessiveInlineDocumentationRule | 12 | 8 of 9 assC AI | broader catch |
| HasEmptyMethodBodyRule | 1 | 1 of 9 assC AI | exact |

**Zero regression** in existing rules between baseline and post sweeps.

## Key decisions

- **`category` PropertyDescriptor skipped on the new rules.** None of the handover proposals supplied a category mapping, and `StudentFeedbackRenderer.getCategoryForRule` falls back to "uncategorized" gracefully. Adding the descriptor is a follow-up if these rules need to surface in `student`/`handover` renderers.
- **Used canonical `.kt` files from `temp/handover/New Kotlin Rules/`, not the simplified inline code in `Zita_Handover.md`.** The doc's inline code was a teaching aid; the real implementations are AST-based and substantially more robust (e.g. `HasRandomDirectionPatternRule` is 100+ lines of AST traversal vs the doc's 12-line regex sketch).
- **Single-signal `HasExcessiveInlineDocumentationRule`.** Original proposal had two heuristics (narration count + comment-to-code ratio). The ratio computation needed source-text access that PMD doesn't expose for our synthesized `Processing.pde` (Zita feeds via `ReaderDataSource`, no on-disk file). Dropping the ratio kept the rule cheap; if precision suffers, the ratio can be re-added by walking AST line spans.
- **Python pipeline kept defaults relative to repo root.** Original handover had Windows-only hardcoded paths (`Internship/Projects/ZITA/Zita/...`); the port accepts 1/2/4-arg invocations and assumes invocation from the repo root.

## What didn't work / failed approaches

1. **Text-based AST reconstruction in `HasRandomDirectionChangePatternRule` (first attempt).**
   First implementation collected `node.image` recursively from the `ASTIfStatement` and ran regex over the result. **Got 0 hits.** PMD's `Node.image` only returns identifier and literal text — operators (`<`, `*=`, `-`) and parens are never captured because they're encoded as the *type* of the node, not the image. Rewrote using direct AST traversal: `findDescendantsOfType(ASTRelationalExpression)` for the comparison, `findDescendantsOfType(ASTAssignmentOperator)` for `*=`, walked for unary minus. Got 13 hits (correct).

2. **`HasExcessiveInlineDocumentationRule` first attempt used `node.getSourceCodeFile()?.readText()`.**
   That method does not exist on the `ASTCompilationUnit` interface in PMD 6.35. Compiler error. Dropped the source-file approach entirely; switched to scanning `node.comments` only.

3. **`PropertyFactory.intProperty(...).require(...)` with a lambda.**
   In PMD 6.35 `.require(...)` takes a `PropertyConstraint<T>` instance, not a `(T) -> String?` lambda. Removed the constraints.

4. **`String.lowercase()`.**
   Kotlin 1.3.70 doesn't have `lowercase()` (added in 1.5). Use `toLowerCase()`. Tom's handover doc warned about this exact pitfall in section 4.8 ("Known build gotchas"); I rediscovered it the hard way before remembering.

5. **`mvn clean` wiped baseline JAR.**
   Saved `target/Zita-baseline.jar`, then ran `mvn clean package` to rebuild the post-integration JAR — `clean` deleted both. Recovered by stashing changes, building baseline again, copying to `/tmp/Zita-baseline-snapshot.jar`, restoring changes, rebuilding post. Lesson: stash baseline outside `target/` from the start.

6. **`bash basename` collisions in regression sweep.**
   Initial driver used `basename "$dir"` for output filenames. AssessmentC has `Try 1`/`Try 2`/`Try 3` directories under both `Claude/`, `GPT/`, and `Gemini/` parents — 9 dirs collapsed to 3 output files. Fixed by encoding the full relative path: `echo "${dir#prefix}" | tr '/' '@' | tr ' ' '_'`.

7. **Baseline JAR crashed on shipped `rules.xml`.**
   Passed `--rules src/main/resources/rulesets/rules.xml` (already containing the 4 new rule entries) to the baseline JAR, which doesn't have those classes. Crash. Fixed by extracting the bundled `rulesets/rules.xml` from the baseline JAR via `unzip -p` and using that for the baseline run.

## Reusable patterns for future work

- **Baseline-vs-post regression**: store the pre-change JAR outside `target/` (e.g. `/tmp`) before any `mvn clean`. Extract bundled config files from the JAR rather than passing on-disk versions, so the baseline run uses the baseline's view of the world.
- **Sample naming**: when sweeping a directory tree where leaf names collide, encode the full relative path with `tr '/' '@' | tr ' ' '_'`.
- **PMD rule that needs both AST shape and source text**: ASTs in PMD 6.x do NOT give you source text directly when the input is a Reader (Zita's case). Either constrain the rule to AST-only, or add a one-time read from `RuleContext`'s data source if the deployment is file-based.
- **Kotlin 1.3.70 stdlib**: stick to pre-1.5 APIs. `toLowerCase()` not `lowercase()`, no trailing commas in `listOf(...)`, no `String.replaceFirstChar`. Tom's handover § 4.8 has the full list.

## Follow-ups deferred

- Tune `HasExcessiveInlineDocumentationRule` thresholds (current default `narrationThreshold = 3`) against a real cohort to measure false-positive rate on instructor-recommended commenting.
- Add `category` PropertyDescriptors to the 9 new rules if they need to appear under specific buckets in `student`/`handover` renderers.
- Open upstream PR for these 9 rules + the pipeline port — currently only on local research branch. Depends on upstream PRs #10 and #11 landing first (otherwise the diff includes the simulation merges).
- Consider adding a JUnit-driven regression suite that invokes the JAR against `temp/handover/ExamplePrograms` and asserts hit counts per rule. Would catch the kind of regressions the manual sweep caught here.
