# Adding Custom Rules

A Zita rule is a PMD `AbstractJavaRule` written in Kotlin (or Java) plus a small amount of metadata wired into two registration files. This guide walks the path from "I want to flag pattern X" to "the rule fires when I run the JAR."

## Quick start — `scripts/new_rule.sh`

The scaffolder is the recommended path. One command generates the Kotlin stub, appends the `rules.xml` entry, and writes the category mapping line — leaving you to fill in the `visit()` body.

```sh
scripts/new_rule.sh \
  HasFooRule \                             # 1. RuleName  — PascalCase, conventionally ends in "Rule".
  "minimum.Basic Functionality" \          # 2. category  — "<bucket>.<subcategory>" (see "Choosing category and priority" below);
                                           #                pass "none" to skip the category property/mapping entirely.
  3 \                                      # 3. priority  — PMD priority 1 (highest) – 5 (lowest); use 3 unless you have a reason not to.
  "One-sentence student-facing message."   # 4. message   — what the student sees when the rule fires.
```

Run `scripts/new_rule.sh --help` (or `-h`, or no args) to print the same usage block at any time. The script refuses to overwrite an existing `.kt` file or duplicate-register a rule, and validates RuleName / priority / category bucket before touching anything on disk.

After scaffolding:

1. Open the generated `src/main/kotlin/nl/utwente/processing/pmd/rules/<RuleName>.kt` and replace the `// TODO` body with your check (see [Implementing the visitor](#implementing-the-visitor)).
2. Build: `mvn -B clean package`.
3. Run against a known-violating sketch (see [Verifying the rule fires](#verifying-the-rule-fires)).

The "[Anatomy of what gets generated](#anatomy-of-what-gets-generated)" section below covers what each touched file looks like, in case you want to register a rule manually or debug a stale entry.

## Choosing category and priority

### Category buckets and subcategories

The `category` argument is `<bucket>.<subcategory>`. The bucket is enforced (`minimum`, `mastery`, or `none`); the subcategory is free-form text used as a grouping label by the `student` and `handover` renderers, so a typo silently creates a new bucket. Match an existing subcategory when one fits; coin a new one only when nothing matches.

Buckets and subcategories currently in use across the ruleset (extracted from `rule-category-mapping.properties`):

| Bucket | Subcategories in use |
|--------|----------------------|
| `minimum` | `submission`, `Basic Functionality`, `Basic Variables & Arithmetic`, `Control Flow`, `Methods`, `Arrays`, `Classes`, `Event Handling` |
| `mastery` | `Code Style`, `Design` |

Pass `none` to skip the category entirely — the rule then surfaces only in the stock PMD renderers (`html`/`json`/`csv`) and not in the `student`/`handover` reports.

### Priority

PMD priority is an integer 1–5, where 1 is highest. `PMDRunner` pins the floor at `LOW` so all five values are honored. Distribution across the existing ruleset:

| Value | PMD label | Existing rules | When to use |
|-------|-----------|----------------|-------------|
| 1 | `HIGH` | 3 | Hard-fail conditions — sketch doesn't compile, submission incomplete. |
| 2 | `MEDIUM_HIGH` | 4 | Major correctness or completeness issues. |
| 3 | `MEDIUM` | 47 (≈85%) | The de-facto default. Use unless you have a reason not to. |
| 4 | `MEDIUM_LOW` | 2 | Soft style nudges, optional improvements. |
| 5 | `LOW` | 0 | Currently unused; reserved. |

Pick `3` unless the rule clearly belongs at one of the extremes.

## Implementing the visitor

The simplest possible rule — fire on every compilation unit that has zero variable declarations — looks like this (real code from `HasVariableRule.kt`):

```kotlin
package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

class HasVariableRule : AbstractJavaRule() {
    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        val hasVariable = node
            .findDescendantsOfType(ASTVariableDeclarator::class.java)
            .isNotEmpty()
        if (!hasVariable) {
            addViolationWithMessage(data, node, message, 0, 0)
        }
        return super.visit(node, data)
    }
}
```

Key points:

- **Base class.** Extend `AbstractJavaRule` for most rules. Extend `nl.utwente.processing.pmd.AbstractProcessingRule` (Java) when you need the 7-arg `addViolationWithMessage(data, node, msg, beginLine, endLine, args)` overload that lets you report violations on an explicit line range without the node's own location.
- **Visitor pattern.** Override `visit(node: ASTSomeNodeType, data: Any?): Any?` for whichever AST node types matter to your check. Always return `super.visit(node, data)` so the traversal continues.
- **Reporting.** Use `addViolationWithMessage(data, node, message, beginLine, endLine)` with `0, 0` to attribute the violation to the node's location, or specific line numbers when you want to override (rarely needed in practice).
- **Don't read source text directly.** Zita synthesizes a single Java compilation unit out of every `.pde` file in the project and feeds PMD a `ReaderDataSource` — `node.beginLine` is the line in that synthesized unit, which the renderers map back to the originating `.pde`. Code that tries to read the source file off disk will not find the original.

### Useful helpers

These live alongside the rules and are reused across the codebase:

- `nl.utwente.processing.pmd.symbols.ProcessingApplet` — enumerates Processing's lifecycle and event method signatures (e.g. `EVENT_METHOD_SIGNATURES`). Use this when distinguishing user-defined methods from Processing-provided callbacks like `setup`, `draw`, `mousePressed`.
- `nl.utwente.processing.pmd.utils.ExpressionUtils` — expression-shape predicates (literal detection, method-call kinds, etc.).
- `nl.utwente.processing.pmd.utils.ScopeUtils` — scope/symbol-table queries.
- `nl.utwente.processing.pmd.utils.Utils` — general AST helpers.

## Anatomy of what gets generated

Three files are involved for a typical rule that should appear in the `student` and `handover` renderers (rules surfaced only by the stock PMD renderers `html`/`json`/`csv` need just the first two — pass `category=none` to the scaffolder to skip the third):

| File | What the scaffolder writes |
|------|---------------------------|
| `src/main/kotlin/nl/utwente/processing/pmd/rules/<RuleName>.kt` | The visitor — Kotlin class extending `AbstractJavaRule`. Includes the `category` `PropertyDescriptor` boilerplate when you pass a real category, omitted when you pass `none`. The `visit()` body is a `// TODO` you must fill in. |
| `src/main/resources/rulesets/rules.xml` | A new `<rule>` block spliced in before `</ruleset>`, carrying name, message, class, priority, and (when applicable) the `<property name="category" value="<bucket>"/>` line. |
| `src/main/resources/rule-category-mapping.properties` | A `<RuleName>=<bucket>.<subcategory>` line. Only written when category ≠ `none`. |

### The `category` PropertyDescriptor (why both files matter)

If your rule should appear in the `student` or `handover` renderers, the rule class must declare a `category` `PropertyDescriptor` so PMD will accept the `<property name="category" .../>` line in `rules.xml`. The scaffolder generates this automatically; the pattern (from `HasHeaderCommentRule.kt`) is:

```kotlin
class HasFooRule : AbstractJavaRule() {
    companion object {
        private val CATEGORY: PropertyDescriptor<String> =
            PropertyFactory.stringProperty("category")
                .desc("Rule category")
                .defaultValue("default")
                .build()
    }

    init {
        definePropertyDescriptor(CATEGORY)
    }

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        // ...
        return super.visit(node, data)
    }
}
```

Forgetting the descriptor is the single most common cause of "every renderer crashes at PMDRunner construction" — `rules.xml` references a property the rule class doesn't define, and PMD aborts. (Two upstream rules shipped with this bug; see `docs/PROGRESS.md` Known Issues.)

### Manual registration (when you skip the scaffolder)

Append before `</ruleset>` in `src/main/resources/rulesets/rules.xml`:

```xml
<rule name="HasFooRule"
      language="java"
      message="One-sentence student-facing message."
      class="nl.utwente.processing.pmd.rules.HasFooRule">
    <description>What the rule checks.</description>
    <priority>3</priority>
    <properties>
        <property name="category" value="minimum"/>
    </properties>
</rule>
```

- `language="java"` is required — Zita feeds PMD synthesized Java, not Processing.
- The `<properties>` block is needed only when the rule defines a `category` `PropertyDescriptor`. Stock-PMD-renderer-only rules can skip it.
- An `<example>` block is optional but recommended — it shows up in PMD's HTML report.

Then add a line in `src/main/resources/rule-category-mapping.properties` (Title Case subcategory, match existing entries):

```properties
HasFooRule=minimum.Basic Functionality
```

## Verifying the rule fires

There is no automated test suite. Verification is empirical:

```sh
# 1. Build
mvn -B clean package

# 2. Run against a sketch you expect to violate (or pass)
java -jar target/Zita.jar \
  --project examples/02-violations \
  --rules src/main/resources/rulesets/rules.xml \
  --renderer zita

# 3. Confirm via JSON for grep-ability
java -jar target/Zita.jar \
  --project examples/02-violations \
  --rules src/main/resources/rulesets/rules.xml \
  --renderer json | python3 -m json.tool | grep -A3 HasFooRule
```

For multi-sample regression checking, the pattern used during development was a Bash sweep over `temp/handover/ExamplePrograms/*.pde` comparing per-rule hit counts against a known baseline. See `docs/PROGRESS.md` Plan 03 for the recipe.

## Common pitfalls

- **Missing `PropertyDescriptor`.** `rules.xml` declares `<property name="category"/>` but the Kotlin class never calls `definePropertyDescriptor(CATEGORY)`. Symptom: `PMDRunner` construction throws on startup; every renderer fails. The scaffolder generates the descriptor automatically — only an issue if you hand-edit.
- **Wrong line numbers.** PMD reports the line in the synthesized Java unit. The accumulating renderers (`zita`, `student`, `handover`) call `ProcessingProject.mapJavaProjectLineNumber` to translate back to `.pde`; stock PMD renderers do not. If you're debugging line offsets, run with `--renderer json` and read the raw line, then map mentally.
- **Reading file contents off disk.** Don't. Use the AST node tree (`node.image`, `node.findDescendantsOfType`, etc.) — the source you're analyzing is the synthesized Java string, not the `.pde` file.
- **Forgetting `super.visit(node, data)`.** The traversal stops, deeper nodes are not visited, and rules that combine multiple node types silently miss matches.
- **Forgetting the `rule-category-mapping.properties` entry.** The rule fires but is silently dropped from the `student`/`handover` renderer output. The scaffolder writes this line for you when category ≠ `none`.

## Where to look next

- Existing rules: `src/main/kotlin/nl/utwente/processing/pmd/rules/` — over 50 examples covering different visitor patterns and reporting strategies.
- Renderer behavior: `src/main/java/nl/utwente/renderers/` — the `zita`, `student`, and `handover` renderers each consume violations differently.
- The synthetic-Java conversion: `nl/utwente/processing/ProcessingProject.toJava` — read this before writing rules that depend on file structure or imports.
