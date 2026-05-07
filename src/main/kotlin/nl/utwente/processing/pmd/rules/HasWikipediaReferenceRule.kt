package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

/**
 * Detects Wikipedia URLs cited as inspiration source in comments.
 * Common AI-codegen tell — LLMs frequently default to Wikipedia for
 * references when asked to credit a source.
 */
class HasWikipediaReferenceRule : AbstractJavaRule() {

    companion object {
        val WIKIPEDIA_PATTERN = Regex("""wikipedia\.org""", RegexOption.IGNORE_CASE)
    }

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        val comments = node.comments ?: return super.visit(node, data)

        for (comment in comments) {
            val text = comment.image ?: continue
            if (WIKIPEDIA_PATTERN.containsMatchIn(text)) {
                addViolationWithMessage(data, node, "Wikipedia reference found in comment.")
                return super.visit(node, data)
            }
        }
        return super.visit(node, data)
    }
}
