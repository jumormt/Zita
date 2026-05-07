#!/usr/bin/env bash
# new_rule.sh — scaffold a new Zita PMD rule.
#
# Generates:
#   1. src/main/kotlin/nl/utwente/processing/pmd/rules/<RuleName>.kt
#   2. an entry in    src/main/resources/rulesets/rules.xml (before </ruleset>)
#   3. an entry in    src/main/resources/rule-category-mapping.properties
#
# Usage:
#   scripts/new_rule.sh <RuleName> <category> <priority> <message>
#
# Example:
#   scripts/new_rule.sh HasFooRule "minimum.Basic Functionality" 3 \
#       "Sketch should do foo at least once."
#
# Arguments:
#   RuleName   PascalCase, conventionally ends in "Rule" (e.g. HasFooRule).
#   category   Bucket + subcategory for the student/handover renderers,
#              dot-separated. Bucket must be "minimum" or "mastery".
#              Pass "none" to skip the category property and the mapping
#              entry (rule will only show in stock PMD renderers).
#   priority   PMD priority 1 (highest) - 5 (lowest). Most rules use 3.
#   message    Student-facing message. Quote it.
#
# Refuses to overwrite existing files. Run from the repo root.

set -euo pipefail

print_usage() {
    sed -n '2,28p' "$0" | sed 's/^# \{0,1\}//'
}

if [[ $# -eq 1 && ( "$1" == "--help" || "$1" == "-h" ) ]]; then
    print_usage
    exit 0
fi

if [[ $# -ne 4 ]]; then
    print_usage
    exit 1
fi

RULE_NAME="$1"
CATEGORY="$2"
PRIORITY="$3"
MESSAGE="$4"

# --- validation ------------------------------------------------------------

if ! [[ "$RULE_NAME" =~ ^[A-Z][A-Za-z0-9]+$ ]]; then
    echo "error: RuleName must be PascalCase (got: $RULE_NAME)" >&2
    exit 1
fi

if ! [[ "$PRIORITY" =~ ^[1-5]$ ]]; then
    echo "error: priority must be 1-5 (got: $PRIORITY)" >&2
    exit 1
fi

USE_CATEGORY=1
CATEGORY_BUCKET=""
if [[ "$CATEGORY" == "none" ]]; then
    USE_CATEGORY=0
else
    CATEGORY_BUCKET="${CATEGORY%%.*}"
    if [[ "$CATEGORY_BUCKET" != "minimum" && "$CATEGORY_BUCKET" != "mastery" ]]; then
        echo "error: category bucket must be 'minimum' or 'mastery' (got: $CATEGORY_BUCKET)" >&2
        echo "       full category was: $CATEGORY" >&2
        exit 1
    fi
    if [[ "$CATEGORY" != *.* ]]; then
        echo "error: category must be '<bucket>.<subcategory>' (got: $CATEGORY)" >&2
        exit 1
    fi
fi

# --- path resolution -------------------------------------------------------

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KT_FILE="$REPO_ROOT/src/main/kotlin/nl/utwente/processing/pmd/rules/${RULE_NAME}.kt"
RULES_XML="$REPO_ROOT/src/main/resources/rulesets/rules.xml"
CAT_PROPS="$REPO_ROOT/src/main/resources/rule-category-mapping.properties"

if [[ -e "$KT_FILE" ]]; then
    echo "error: $KT_FILE already exists" >&2
    exit 1
fi

if grep -q "name=\"${RULE_NAME}\"" "$RULES_XML"; then
    echo "error: $RULES_XML already declares a rule named '$RULE_NAME'" >&2
    exit 1
fi

if [[ $USE_CATEGORY -eq 1 ]] && grep -q "^${RULE_NAME}=" "$CAT_PROPS"; then
    echo "error: $CAT_PROPS already maps '$RULE_NAME'" >&2
    exit 1
fi

# --- generate Kotlin file --------------------------------------------------

if [[ $USE_CATEGORY -eq 1 ]]; then
    cat > "$KT_FILE" <<EOF
package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.properties.PropertyDescriptor
import net.sourceforge.pmd.properties.PropertyFactory

/**
 * TODO: describe what this rule checks.
 */
class ${RULE_NAME} : AbstractJavaRule() {

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
        // TODO: walk the AST and decide whether to report a violation.
        // val matches = node.findDescendantsOfType(ASTSomeNode::class.java)
        // if (matches.isEmpty()) addViolationWithMessage(data, node, message, 0, 0)
        return super.visit(node, data)
    }
}
EOF
else
    cat > "$KT_FILE" <<EOF
package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule

/**
 * TODO: describe what this rule checks.
 */
class ${RULE_NAME} : AbstractJavaRule() {

    override fun visit(node: ASTCompilationUnit, data: Any?): Any? {
        // TODO: walk the AST and decide whether to report a violation.
        return super.visit(node, data)
    }
}
EOF
fi

echo "created  $KT_FILE"

# --- append to rules.xml ---------------------------------------------------

# We splice a new <rule> block in just before the final </ruleset>. Using awk
# rather than sed -i because the multi-line replacement is fiddly under sed.

XML_SNIPPET=$(mktemp)
trap 'rm -f "$XML_SNIPPET"' EXIT

if [[ $USE_CATEGORY -eq 1 ]]; then
    cat > "$XML_SNIPPET" <<EOF
    <rule name="${RULE_NAME}"
          language="java"
          message="${MESSAGE}"
          class="nl.utwente.processing.pmd.rules.${RULE_NAME}">
        <description>TODO: describe what this rule checks.</description>
        <priority>${PRIORITY}</priority>
        <properties>
            <property name="category" value="${CATEGORY_BUCKET}"/>
        </properties>
    </rule>

EOF
else
    cat > "$XML_SNIPPET" <<EOF
    <rule name="${RULE_NAME}"
          language="java"
          message="${MESSAGE}"
          class="nl.utwente.processing.pmd.rules.${RULE_NAME}">
        <description>TODO: describe what this rule checks.</description>
        <priority>${PRIORITY}</priority>
    </rule>

EOF
fi

awk -v snippet_file="$XML_SNIPPET" '
    /<\/ruleset>/ && !done {
        while ((getline line < snippet_file) > 0) print line
        close(snippet_file)
        done = 1
    }
    { print }
' "$RULES_XML" > "$RULES_XML.tmp"
mv "$RULES_XML.tmp" "$RULES_XML"
echo "updated  $RULES_XML"

# --- append to rule-category-mapping.properties ----------------------------

if [[ $USE_CATEGORY -eq 1 ]]; then
    # Ensure file ends with a newline before appending.
    if [[ -s "$CAT_PROPS" ]] && [[ "$(tail -c1 "$CAT_PROPS"; echo x)" != $'\nx' ]]; then
        printf '\n' >> "$CAT_PROPS"
    fi
    printf '%s=%s\n' "$RULE_NAME" "$CATEGORY" >> "$CAT_PROPS"
    echo "updated  $CAT_PROPS"
fi

# --- summary ---------------------------------------------------------------

cat <<EOF

Next steps:
  1. Implement the visit() body in $KT_FILE
  2. mvn -B clean package
  3. java -jar target/Zita.jar \\
         --project examples/02-violations \\
         --rules src/main/resources/rulesets/rules.xml \\
         --renderer zita

See docs/CUSTOM_RULES.md for the full walkthrough.
EOF
