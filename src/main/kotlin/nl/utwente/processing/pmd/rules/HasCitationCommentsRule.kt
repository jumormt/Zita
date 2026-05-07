package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

/**
 * Detects AI citation markers left in code comments.
 * Flags patterns like [cite: 4], [cite: 36, 52], etc.
 */
class HasCitationCommentsRule : AbstractJavaRule() {

    companion object {
        val CITATION_PATTERN = Regex("""\[cite:\s*\d+(\s*,\s*\d+)*\s*\]""", RegexOption.IGNORE_CASE)
    }

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        val comments = node.comments ?: return super.visit(node, data)

        for (comment in comments) {
            val commentText = comment.image ?: continue

            if (CITATION_PATTERN.containsMatchIn(commentText)) {
                addViolationWithMessage(
                    data,
                    node,
                    "AI citation marker detected: ${CITATION_PATTERN.find(commentText)?.value}"
                )
                return super.visit(node, data)
            }
        }

        return super.visit(node, data)
    }
}