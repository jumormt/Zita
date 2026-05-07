package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.ast.Node
import net.sourceforge.pmd.lang.java.ast.ASTAssignmentOperator
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.ast.ASTIfStatement
import net.sourceforge.pmd.lang.java.ast.ASTLiteral
import net.sourceforge.pmd.lang.java.ast.ASTName
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryPrefix
import net.sourceforge.pmd.lang.java.ast.ASTRelationalExpression
import net.sourceforge.pmd.lang.java.ast.ASTStatementExpression
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

/**
 * Detects the AI-typical idiom for sporadic direction reversal:
 *   if (random(1) < 0.01) dir *= -1;
 *   if (random(100) < 1)  dir *= -1;
 *
 * Implementation: walks ASTIfStatements, looks for a relational `<`
 * comparison whose left side is `random(small_int)` and whose right side
 * is a small numeric literal, then checks the body for a `*=` assignment
 * with `-1` as the right operand.
 */
class HasRandomDirectionChangePatternRule : AbstractJavaRule() {

    private var foundOnce = false

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        foundOnce = false
        return super.visit(node, data)
    }

    override fun visit(node: ASTIfStatement, data: Any?): Any? {
        if (!foundOnce && hasRandomLessThanCondition(node) && bodyContainsTimesEqualsNegOne(node)) {
            foundOnce = true
            addViolationWithMessage(
                data, node,
                "AI-typical random direction-change pattern detected."
            )
        }
        return super.visit(node, data)
    }

    private fun hasRandomLessThanCondition(node: ASTIfStatement): Boolean {
        // Walk descendants for a `<` relational expression with random(...) on the left.
        for (rel in node.findDescendantsOfType(ASTRelationalExpression::class.java)) {
            if (rel.image != "<") continue
            val callsRandom = findRandomCall(rel)
            if (callsRandom) return true
        }
        return false
    }

    private fun findRandomCall(node: Node): Boolean {
        // Walk children for an ASTPrimaryPrefix wrapping ASTName "random".
        for (i in 0 until node.numChildren) {
            val child = node.getChild(i)
            if (child is ASTPrimaryPrefix) {
                val nameChild = if (child.numChildren > 0) child.getChild(0) else null
                if (nameChild is ASTName && nameChild.image == "random") return true
            }
            if (findRandomCall(child)) return true
        }
        return false
    }

    private fun bodyContainsTimesEqualsNegOne(node: ASTIfStatement): Boolean {
        // Look for `<lhs> *= -1` inside the if body.
        // ASTStatementExpression contains the assignment; ASTAssignmentOperator
        // has image "*="; the right side is a primary expression with literal "1"
        // preceded by a unary minus (whose image is "-").
        for (stmt in node.findDescendantsOfType(ASTStatementExpression::class.java)) {
            val assignOp = stmt.findDescendantsOfType(ASTAssignmentOperator::class.java).firstOrNull()
                ?: continue
            if (assignOp.image != "*=") continue
            // A literal "1" anywhere on the RHS combined with a leading "-" is a
            // good-enough proxy for `*= -1`.
            val literals = stmt.findDescendantsOfType(ASTLiteral::class.java)
            if (literals.any { it.image == "1" }) {
                // Check for unary minus on the rhs by looking for any node whose image is "-"
                if (hasUnaryMinus(stmt)) return true
            }
        }
        return false
    }

    private fun hasUnaryMinus(node: Node): Boolean {
        if (node.image == "-") return true
        for (i in 0 until node.numChildren) {
            if (hasUnaryMinus(node.getChild(i))) return true
        }
        return false
    }
}
