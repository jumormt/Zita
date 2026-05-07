package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.ast.Node
import net.sourceforge.pmd.lang.java.ast.*
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

class HasRandomDirectionPatternRule : AbstractJavaRule() {

    private var foundPattern = false

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        foundPattern = false
        return super.visit(node, data)
    }

    override fun visit(node: ASTConditionalExpression, data: Any?): Any? {
        if (foundPattern) return super.visit(node, data)

        if (isRandomDirectionPattern(node)) {
            foundPattern = true
            addViolationWithMessage(data, node, "AI-typical random direction pattern detected")
        }

        return super.visit(node, data)
    }

    private fun isRandomDirectionPattern(node: ASTConditionalExpression): Boolean {
        if (node.numChildren < 3) return false

        if (!containsRandomOneCall(node.getChild(0))) return false

        val thenVal = getBranchValue(node.getChild(1))
        val elseVal = getBranchValue(node.getChild(2))

        return (thenVal == 1 && elseVal == -1) || (thenVal == -1 && elseVal == 1)
    }

    // Recursively checks if node subtree contains random(1)
    private fun containsRandomOneCall(node: Node): Boolean {
        if (node is ASTPrimaryExpression) {
            var hasRandomName = false
            var hasOneArg = false

            for (i in 0 until node.numChildren) {
                val child = node.getChild(i)
                if (child is ASTPrimaryPrefix) {
                    val nameNode = child.getChild(0)
                    if (nameNode is ASTName && nameNode.image == "random") {
                        hasRandomName = true
                    }
                }
                if (child is ASTPrimarySuffix) {
                    val argsNode = if (child.numChildren > 0) child.getChild(0) else null
                    if (argsNode is ASTArguments && argsNode.argumentCount == 1) {
                        // Check the single argument is the literal 1
                        val lit = findLiteral(argsNode)
                        if (lit != null && lit.image == "1") {
                            hasOneArg = true
                        }
                    }
                }
            }

            if (hasRandomName && hasOneArg) return true
        }

        for (i in 0 until node.numChildren) {
            if (containsRandomOneCall(node.getChild(i))) return true
        }
        return false
    }

    // Returns 1 or -1 if the branch resolves to those values, otherwise null
    private fun getBranchValue(node: Node): Int? {
        if (node is ASTLiteral && node.image == "1") return 1

        if (node is ASTUnaryExpression && node.image == "-") {
            val lit = findLiteral(node)
            if (lit != null && lit.image == "1") return -1
        }

        // Recurse one level for wrapped expressions
        if (node.numChildren == 1) return getBranchValue(node.getChild(0))

        return null
    }

    private fun findLiteral(node: Node): ASTLiteral? {
        if (node is ASTLiteral) return node
        for (i in 0 until node.numChildren) {
            val result = findLiteral(node.getChild(i))
            if (result != null) return result
        }
        return null
    }
}