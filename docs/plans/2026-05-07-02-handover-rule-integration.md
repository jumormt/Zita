# Plan: Integrate 4 handover AI-detection rules into Zita

**Epic:** E1 (AI-detection rule integration)
**Design:** none — direct port from `temp/handover/New Kotlin Rules/`. The 4 .kt files in the handover are already production-shaped (AST-based, not the simplified regex shown inline in `Zita_Handover.md`).

## Summary

Copy the 4 implemented Kotlin rules from the handover into the live source tree, register them in `rules.xml`, verify the project still builds, and smoke-test against one sample sketch. Defer false-positive analysis to plan 03 (regression baseline).

**Decisions locked in:**
- Use the canonical `.kt` files (the AST implementations), not the simplified regex inlined in `Zita_Handover.md`.
- Skip the `category` property — none of the 4 new rules has documented category mapping in the handover, and `StudentFeedbackRenderer` falls back to "uncategorized" gracefully.
- Skip `rule-category-mapping.properties` for now — these rules will surface in the default `zita`/`json`/`csv` renderers; categorization can be added later if `student`/`handover` renderers need it.
- Priority for all 4 rules: `2` (matches `HasPlaceholderAuthorRule` example in the handover doc).

---

## Phase 1: Copy source files

### [ ] Task 1.1: Copy 4 .kt files
- [ ] `cp temp/handover/New\ Kotlin\ Rules/HasPlaceholderAuthorRule.kt src/main/kotlin/nl/utwente/processing/pmd/rules/`
- [ ] Same for `HasDecorativeSectionCommentsRule.kt`
- [ ] Same for `HasCitationCommentsRule.kt`
- [ ] Same for `HasRandomDirectionPatternRule.kt`
- [ ] Confirm package declaration is `nl.utwente.processing.pmd.rules` (it is per earlier inspection)

## Phase 2: Register in rules.xml

### [ ] Task 2.1: Append 4 `<rule>` blocks
- [ ] Add HasPlaceholderAuthorRule entry — message "Placeholder author text detected in comment.", priority 2
- [ ] Add HasDecorativeSectionCommentsRule entry — message "Decorative section comment dividers detected.", priority 2
- [ ] Add HasCitationCommentsRule entry — message "AI citation marker detected in comment.", priority 2
- [ ] Add HasRandomDirectionPatternRule entry — message "AI-typical random direction ternary pattern detected.", priority 2

## Phase 3: Build verification

### [ ] Task 3.1: Clean build
- [ ] `mvn -B clean package` → exit 0
- [ ] `unzip -l target/Zita.jar | grep -E "(HasPlaceholderAuthorRule|HasDecorativeSectionCommentsRule|HasCitationCommentsRule|HasRandomDirectionPatternRule)\.class"` → 4 hits

## Phase 4: Smoke test

### [ ] Task 4.1: Run against a known-Claude sample
- [ ] `java -jar target/Zita.jar --project temp/handover/ExamplePrograms/AssessmentC/Persona\ 2/assC-P2-Claude1 --rules src/main/resources/rulesets/rules.xml --renderer zita`
- [ ] Confirm at least one of the new rules fires (Claude submissions hit decorative dividers, placeholder authors, and random direction patterns per handover evidence)

## Verification

- [ ] All 4 .kt files compile
- [ ] All 4 rules load without `IllegalArgumentException` at PMDRunner construction
- [ ] At least one new rule fires on a known-AI sketch
- [ ] PROGRESS.md updated with results
- [ ] Plan 03 (regression baseline) ready to start
