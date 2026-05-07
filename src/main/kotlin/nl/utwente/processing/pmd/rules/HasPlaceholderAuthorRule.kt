package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

/**
 * Detects placeholder text in author/name fields within comments.
 * Flags patterns like [Your Name], <your name>, TODO: add name, etc.
 */
class HasPlaceholderAuthorRule : AbstractJavaRule() {

    companion object {
        val PLACEHOLDER_PATTERNS = listOf(
            Regex("""\[Your\s*Name\]""", RegexOption.IGNORE_CASE),
            Regex("""\[Name\]""", RegexOption.IGNORE_CASE),
            Regex("""\[Author\]""", RegexOption.IGNORE_CASE),
            Regex("""<your\s*name>""", RegexOption.IGNORE_CASE),
            Regex("""<name\s*here>""", RegexOption.IGNORE_CASE),
            Regex("""TODO:?\s*(add|enter|insert)?\s*(your)?\s*name""", RegexOption.IGNORE_CASE),
            Regex("""Author:?\s*\[.*\]""", RegexOption.IGNORE_CASE),
            Regex("""Name:?\s*\[.*\]""", RegexOption.IGNORE_CASE)
        )
    }

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        val comments = node.comments ?: return super.visit(node, data)

        for (comment in comments) {
            val commentText = comment.image ?: continue

            for (pattern in PLACEHOLDER_PATTERNS) {
                if (pattern.containsMatchIn(commentText)) {
                    addViolationWithMessage(
                        data,
                        node,
                        "Placeholder author text detected: ${pattern.find(commentText)?.value}"
                    )
                    return super.visit(node, data)
                }
            }
        }

        return super.visit(node, data)
    }
}