# Adding Custom Rules

A Zita rule is a PMD `AbstractJavaRule` written in Kotlin (or Java) plus a small amount of metadata wired into two registration files. This guide walks through the full path from "I want to flag pattern X" to "the rule fires when I run the JAR."

## Anatomy of a rule

Three files are involved for a typical rule that should appear in the `student` and `handover` renderers:

| File | Purpose |
|------|---------|
| `src/main/kotlin/nl/utwente/processing/pmd/rules/<RuleName>.kt` | The visitor — walks the synthesized Java AST and reports violations. |
| `src/main/resources/rulesets/rules.xml` | PMD ruleset entry: name, message, class, priority, optional `category` property. |
| `src/main/resources/rule-category-mapping.properties` | Maps the rule name to a category bucket (`minimum.X` / `mastery.Y`) for the `student` and `handover` renderers. |

Rules surfaced only by the stock PMD renderers (`html`/`json`/`csv`) need just the first two; the third file is consumed by the project-specific accumulating renderers.

## Authoring the visitor

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

### Rules that need a category property

If your rule should appear in the `student` or `handover` renderers, the rule class must declare a `category` `PropertyDescriptor` so PMD will accept the `<property name="category" .../>` line in `rules.xml`. The standard pattern (from `HasHeaderCommentRule.kt`) is:

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

Forgetting the descriptor is the single most common cause of "every renderer crashes at PMDRunner construction" — `rules.xml` references a property the rule class doesn't define, and PMD aborts. (Two upstream rules shipped with this bug, see PROGRESS.md `Known Issues`.)

## Registering the rule

### `rules.xml` entry

Append before `</ruleset>`:

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
- `<priority>` is 1 (highest) to 5 (lowest). Zita pins the floor at `LOW`, so all values are honored.
- The `<properties>` block is needed only when the rule defines a `category` `PropertyDescriptor` (see above). Stock-PMD-renderer-only rules can skip it.
- An `<example>` block is optional but recommended — it shows up in PMD's HTML report.

### `rule-category-mapping.properties` entry

Add a line in the appropriate section:

```properties
HasFooRule=minimum.Basic Functionality
```

Format: `<RuleName>=<bucket>.<Subcategory>` where `<bucket>` is `minimum` or `mastery`. The subcategory is a free-form string (use Title Case, with spaces — match existing entries). The `student` / `handover` renderers group violations by this label.

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

- **Missing `PropertyDescriptor`.** `rules.xml` declares `<property name="category"/>` but the Kotlin class never calls `definePropertyDescriptor(CATEGORY)`. Symptom: `PMDRunner` construction throws on startup; every renderer fails.
- **Wrong line numbers.** PMD reports the line in the synthesized Java unit. The accumulating renderers (`zita`, `student`, `handover`) call `ProcessingProject.mapJavaProjectLineNumber` to translate back to `.pde`; stock PMD renderers do not. If you're debugging line offsets, run with `--renderer json` and read the raw line, then map mentally.
- **Reading file contents off disk.** Don't. Use the AST node tree (`node.image`, `node.findDescendantsOfType`, etc.) — the source you're analyzing is the synthesized Java string, not the `.pde` file.
- **Forgetting `super.visit(node, data)`.** The traversal stops, deeper nodes are not visited, and rules that combine multiple node types silently miss matches.
- **Forgetting the `rule-category-mapping.properties` entry.** The rule fires but is silently dropped from the `student`/`handover` renderer output.

## Scaffolding script

For new rules, prefer:

```sh
scripts/new_rule.sh HasFooRule "minimum.Basic Functionality" 3 \
  "One-sentence student-facing message."
```

It generates the Kotlin stub, appends the `rules.xml` entry, and adds the category mapping in one shot. See [`scripts/new_rule.sh`](../scripts/new_rule.sh) for arguments.

## Where to look next

- Existing rules: `src/main/kotlin/nl/utwente/processing/pmd/rules/` — over 50 examples covering different visitor patterns and reporting strategies.
- Renderer behavior: `src/main/java/nl/utwente/renderers/` — the `zita`, `student`, and `handover` renderers each consume violations differently.
- The synthetic-Java conversion: `nl/utwente/processing/ProcessingProject.toJava` — read this before writing rules that depend on file structure or imports.
