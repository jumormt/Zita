package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

/**
 * Detects elaborate ASCII dividers in comments.
 * Flags patterns with 10+ repeated characters like -, =, *, etc.
 * Also detects Unicode box-drawing characters.
 */
class HasDecorativeSectionCommentsRule : AbstractJavaRule() {

    companion object {
        val DECORATIVE_PATTERNS = listOf(
            Regex("""-{10,}"""),
            Regex("""={10,}"""),
            Regex("""\*{10,}"""),
            Regex("""#{10,}"""),
            Regex("""~{10,}"""),
            Regex("""_{10,}"""),
            Regex("""\u2500{5,}"""),
            Regex("""\u2550{5,}""")
        )
    }

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        val comments = node.comments ?: return super.visit(node, data)

        for (comment in comments) {
            val commentText = comment.image ?: continue

            for (pattern in DECORATIVE_PATTERNS) {
                if (pattern.containsMatchIn(commentText)) {
                    addViolationWithMessage(
                        data,
                        node,
                        "Decorative section comment detected"
                    )
                    return super.visit(node, data)
                }
            }
        }

        return super.visit(node, data)
    }
}