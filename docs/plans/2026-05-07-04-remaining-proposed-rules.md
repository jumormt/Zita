# Plan: Implement the remaining 5 proposed AI-detection rules

**Epic:** E1 (AI-detection rule integration)
**Design:** none — patterns are documented in `temp/handover/Analysis Scripts/scripts/new_proposed_rules.md` with concrete code-string evidence.

## Summary

Author Kotlin implementations for the 5 rules that exist only as proposals in the handover (not as `.kt` files). Each is a single-pattern detector following the AST visitor + companion-object regex pattern established by the 4 implemented rules.

**Decisions locked in:**
- Implement in priority order from `new_proposed_rules.md`: high (1) → medium (3) → low (1).
- Each rule gets its own commit so the diff per rule stays reviewable.
- Threshold/regex tuning is empirical: implement → run against the 74-sample testbed (plan 03 infrastructure) → adjust → repeat.
- For `HasExcessiveInlineDocumentationRule` (highest false-positive risk), ship behind a tunable property (line count threshold + comment-to-code ratio) rather than a hardcoded heuristic.

---

## Phase 1: Medium-priority detectors (mostly mechanical)

### [ ] Task 1.1: HasRandomDirectionChangePatternRule
- [ ] Detect `random(1) < <small_constant>` followed by `*= -1` or `direction *= -1`
- [ ] Pattern: `if (random(1) < 0.01) dir *= -1;` (or `random(100) < 1` variant per Gemini3 evidence)
- [ ] AST: walk `ASTIfStatement` → check condition is `random(1) < <literal less than 0.1>` AND body contains `*= -1`
- [ ] Hit-rate target from handover: 6/9 assC AI submissions

### [ ] Task 1.2: HasFrameCountMagicNumberRule
- [ ] Detect integer literals (60, 120, 180, 300, 360 = common 60fps multiples) **with comment containing "fps", "seconds", "frames"** on the same or previous line
- [ ] AST: walk `ASTLiteral` for int values in `{60, 120, 180, 240, 300, 360}` → check sibling/parent comment for fps-related keywords
- [ ] Hit-rate target: 5/9 assC submissions

### [ ] Task 1.3: HasWikipediaReferenceRule
- [ ] Single regex on comments: `wikipedia\.org`
- [ ] Trivial — copy `HasCitationCommentsRule.kt` shape, swap regex
- [ ] Hit-rate target: 4/9 assC submissions

## Phase 2: High-noise / careful detectors

### [ ] Task 2.1: HasExcessiveInlineDocumentationRule
- [ ] Heuristic: ratio of comment lines to executable lines exceeds threshold (e.g., > 0.4 across all methods)
- [ ] OR: number of single-line comments where comment text restates a Processing built-in name (e.g., `background(0); // Black background`)
- [ ] Implement both signals; combine with OR; expose tunable thresholds via PMD properties
- [ ] Hit-rate target: 8/9 — but this is the rule most likely to produce false positives on instructor-recommended commenting

## Phase 3: Low-priority / single-evidence detectors

### [ ] Task 3.1: HasEmptyMethodBodyRule
- [ ] AST: walk `ASTMethodDeclaration` → check body is empty (`numChildren == 0` of `ASTBlock`)
- [ ] Skip method overrides (`@Override`) and Processing lifecycle methods (setup, draw, mousePressed, ...) — those legitimately can be empty
- [ ] Hit-rate target: 1/9 — weak signal alone but cheap

## Phase 4: Registration + verification

### [ ] Task 4.1: Add 5 `<rule>` entries to rules.xml
### [ ] Task 4.2: Re-run plan 03's regression sweep with all 9 new rules active
### [ ] Task 4.3: Document false-positive rates per rule in PROGRESS.md

## Verification

- [ ] All 5 .kt files compile (`mvn -B clean package`)
- [ ] All 5 rules load
- [ ] For each, hit rate on the testbed is within ±2 of the handover-documented expected count
- [ ] False-positive rate on human-written reference sketches (`Hopper/`, `FrogHopper/`, AssessmentA references) ≤ 1 per rule
- [ ] Each rule has its own commit; total of 5 commits for this plan
