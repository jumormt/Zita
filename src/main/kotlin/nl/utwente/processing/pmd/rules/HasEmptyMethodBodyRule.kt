package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTBlock
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import nl.utwente.processing.pmd.symbols.ProcessingApplet

/**
 * Detects empty method bodies on user-defined methods.
 * Excludes Processing lifecycle and event handlers (setup, draw,
 * mousePressed, keyPressed, ...) which legitimately may be empty.
 */
class HasEmptyMethodBodyRule : AbstractJavaRule() {

    companion object {
        // Method names that are allowed to be empty by Processing convention.
        val EXEMPT_NAMES: Set<String> by lazy {
            val names = mutableSetOf("setup", "draw")
            ProcessingApplet.EVENT_METHOD_SIGNATURES
                .map { it.substringBefore("(") }
                .forEach { names.add(it) }
            names
        }
    }

    override fun visit(node: ASTMethodDeclaration, data: Any?): Any? {
        val name = node.name
        if (name in EXEMPT_NAMES) return super.visit(node, data)

        val block = node.findDescendantsOfType(ASTBlock::class.java).firstOrNull()
            ?: return super.visit(node, data)
        if (block.numChildren == 0) {
            addViolationWithMessage(
                data, node,
                "Empty user-defined method body: $name()."
            )
        }
        return super.visit(node, data)
    }
}
