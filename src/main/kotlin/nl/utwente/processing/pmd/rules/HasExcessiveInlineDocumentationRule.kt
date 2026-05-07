package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.properties.PropertyDescriptor
import net.sourceforge.pmd.properties.PropertyFactory

/**
 * Detects code where comments restate or trivially narrate adjacent
 * Processing API calls — a near-universal AI-codegen tell (handover hit
 * rate 8/9). Counts "narrating" comments: short comments whose text
 * mentions a Processing built-in name. When the count crosses
 * `narrationThreshold`, the file is flagged.
 *
 * Single-signal version of the proposal — the comment-to-code-line ratio
 * idea is kept in FUTURE.md for a follow-up if needed.
 */
class HasExcessiveInlineDocumentationRule : AbstractJavaRule() {

    companion object {
        private val NARRATION_THRESHOLD: PropertyDescriptor<Int> =
            PropertyFactory.intProperty("narrationThreshold")
                .desc("Minimum number of narrating comments to flag.")
                .defaultValue(3)
                .build()

        // A small set of Processing built-ins commonly narrated in
        // AI-typical comments such as "// black background".
        val NARRATED_APIS = listOf(
            "background", "fill", "stroke", "ellipse", "rect", "line",
            "triangle", "quad", "arc", "size", "frameRate",
            "translate", "rotate", "scale", "text", "image"
        )

        // Match short comments (max ~50 chars of meaningful body) that
        // mention an API word.
        val NARRATION_PATTERNS = NARRATED_APIS.map {
            Regex("""\b$it\b""", RegexOption.IGNORE_CASE)
        }
    }

    init {
        definePropertyDescriptor(NARRATION_THRESHOLD)
    }

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        val comments = node.comments ?: return super.visit(node, data)

        var narrationHits = 0
        for (comment in comments) {
            val text = comment.image?.trim() ?: continue
            // Skip the slashes/asterisks themselves; keep the body short.
            val body = text
                .removePrefix("//")
                .removePrefix("/*")
                .removeSuffix("*/")
                .trim()
            if (body.isEmpty() || body.length > 60) continue
            if (NARRATION_PATTERNS.any { it.containsMatchIn(body) }) {
                narrationHits++
            }
        }

        val limit = getProperty(NARRATION_THRESHOLD)
        if (narrationHits >= limit) {
            addViolationWithMessage(
                data, node,
                "Excessive inline documentation: $narrationHits short comments " +
                "narrate Processing API calls (threshold $limit)."
            )
        }
        return super.visit(node, data)
    }
}
