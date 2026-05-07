package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

/**
 * Detects frame-based timing constants paired with explanatory comments
 * such as "// 3 seconds at 60 fps" or "// frames the X stays vulnerable".
 *
 * Approach: scan all comments for fps/frame-related keywords. False
 * positives are mostly limited to legitimate pedagogical comments that
 * happen to mention "fps", which is an acceptable trade-off given the
 * pattern's strong AI-codegen signal (5/9 hit rate per handover).
 *
 * The integer-literal precondition from the proposal (60/120/180/...)
 * is intentionally dropped — the comment text alone is the more
 * specific signal, and adding a literal-pairing constraint would
 * complicate the rule without improving precision against this corpus.
 */
class HasFrameCountMagicNumberRule : AbstractJavaRule() {

    companion object {
        // Match comments that explain a frame-based duration.
        // Examples seen in the handover testbed:
        //   "// 3 seconds at 60 fps"
        //   "// frames the ghost stays vulnerable"
        //   "// 3 seconds at 60fps"
        val FPS_PATTERN = Regex(
            """\b(\d+\s*fps|frames?\s+(at|of|stays?|until|to|the))\b""",
            RegexOption.IGNORE_CASE
        )
        val SECONDS_AT_FPS_PATTERN = Regex(
            """\d+\s*seconds?\s+at\s+\d+\s*fps""",
            RegexOption.IGNORE_CASE
        )
    }

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        val comments = node.comments ?: return super.visit(node, data)
        for (comment in comments) {
            val text = comment.image ?: continue
            if (FPS_PATTERN.containsMatchIn(text) || SECONDS_AT_FPS_PATTERN.containsMatchIn(text)) {
                addViolationWithMessage(
                    data, node,
                    "Frame-count magic number with fps/frames explanatory comment detected."
                )
                return super.visit(node, data)
            }
        }
        return super.visit(node, data)
    }
}
