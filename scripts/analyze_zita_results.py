#!/usr/bin/env python3
"""
Zita result analyser.

Reads CSV files produced by ``batch_process_zita.py`` (with PMD's CSV
renderer) and writes ``analysis_report.md`` and ``analysis_report.json``
into the same directory.

Usage::

    python scripts/analyze_zita_results.py <dir-of-csv-files>

Typical workflow::

    python scripts/batch_process_zita.py path/to/submissions
    python scripts/analyze_zita_results.py path/to/submissions/batch-analysis/csv
"""

import csv
import json
import sys
from collections import Counter
from pathlib import Path

# Rule taxonomy used to render the categorised report. Rules not in this
# table appear under "Rules Not in Category List" instead of being silently
# dropped. Add new rules here when registering them in rules.xml.
RULES: dict[str, list[str]] = {
    "structure": [
        "HasVariableRule",
        "HasHeaderCommentRule",
        "ProcessingJavaBuildRule",
        "HasUserDefinedMethod",
        "HasStandardProcessingStructure",
        "HasDrawRule",
        "HasSetupRule",
    ],
    "functions": [
        "HasNonVoidFunctionRule",
        "HasFunctionWithParametersRule",
        "LongMethodRule",
        "LongParameterListRule",
    ],
    "classes": [
        "HasUserDefinedClass",
        "HasUserDefinedConstructor",
        "HasClassWithConstructorRule",
        "UsingUserDefinedClass",
        "StatelessClassRule",
        "GodClassRule",
        "HasInheritanceRule",
        "HasAbstractClassOrInterfaceRule",
        "HasTripleNestedClassRule",
        "ClassCallsDrawMethodRule",
    ],
    "control_flow": [
        "HasConditionsRule",
        "HasLoopRule",
        "HasNoElseStatementRule",
        "HasNoBooleanOperatorRule",
        "HasForEachLoopRule",
        "HasArrayIndexForLoop",
    ],
    "events": [
        "HasEventHandlerRule",
        "UsefulEventHandlerRule",
        "EventHandlerNoDeclaredCallRule",
        "EventHandlerNoControlFlowRule",
        "DecentralizedEventHandlingRule",
    ],
    "operators": [
        "HasTernaryOperatorRule",
        "HasModuloOperatorRule",
        "VariableArithmeticRule",
    ],
    "arrays": [
        "HasArrayUsageRule",
    ],
    "drawing": [
        "Has2DShapesRule",
        "DecentralizedDrawingRule",
        "PixelHardcodeIgnoranceRule",
    ],
    "advanced_features": [
        "AdvancedProcessingFunctionRule",
        "ThisKeywordUsageRule",
        "HasAccessModifierRule",
        "HasFinalVariableRule",
        "HasImportStatementRule",
        "HasClassUsageRule",
    ],
    "complexity": [
        "CognitiveComplexity",
        "CyclomaticComplexity",
        "AvoidDeeplyNestedIfStmts",
        "TooManyFields",
        "TooManyMethods",
        "GodClass",
        "CouplingBetweenObjects",
        "LawOfDemeter",
    ],
    "naming": [
        "ClassNamingConventions",
        "FieldNamingConventions",
        "FormalParameterNamingConventions",
        "LocalVariableNamingConventions",
        "MethodNamingConventions",
        "ShortClassName",
        "ShortMethodName",
        "ShortVariable",
        "LongVariable",
        "AvoidFieldNameMatchingMethodName",
        "AvoidFieldNameMatchingTypeName",
    ],
    "code_style": [
        "ControlStatementBraces",
        "FieldDeclarationsShouldBeAtStartOfClass",
        "OneDeclarationPerLine",
        "AtLeastOneConstructor",
        "UnnecessaryConstructor",
    ],
    "best_practices": [
        "AvoidReassigningLoopVariables",
        "AvoidReassigningParameters",
        "ForLoopVariableCount",
        "UnusedAssignment",
        "UnusedFormalParameter",
        "UnusedLocalVariable",
    ],
    "error_prone": [
        "AssignmentInOperand",
        "AvoidLiteralsInIfCondition",
        "IdempotentOperations",
        "MethodWithSameNameAsEnclosingClass",
        "UnconditionalIfStatement",
    ],
    "simplification": [
        "SimplifyBooleanExpressions",
        "SimplifyBooleanReturns",
        "SimplifyConditional",
        "CollapsibleIfStatements",
        "SingularField",
    ],
    "scope": [
        "OutOfScopeStateChangeRule",
    ],
    # AI-codegen detection rules (handover 2026-05-07).
    "ai_detection": [
        "HasPlaceholderAuthorRule",
        "HasDecorativeSectionCommentsRule",
        "HasCitationCommentsRule",
        "HasRandomDirectionPatternRule",
    ],
}


class ZitaAnalyzer:
    def __init__(self) -> None:
        self.assessments: dict[str, list[dict]] = {}

    def load_csv(self, filepath: Path, name: str) -> bool:
        for encoding in ("utf-8", "utf-16", "utf-16-le", "utf-16-be", "latin-1"):
            try:
                with open(filepath, "r", encoding=encoding) as f:
                    self.assessments[name] = list(csv.DictReader(f))
                return True
            except (UnicodeDecodeError, UnicodeError):
                continue
        return False

    def get_rule_counts(self, name: str) -> Counter:
        return Counter(v["Rule"] for v in self.assessments.get(name, []))

    def all_rules_detected(self) -> list[str]:
        rules: set[str] = set()
        for name in self.assessments:
            rules.update(self.get_rule_counts(name).keys())
        return sorted(rules)

    def all_known_rules(self) -> list[str]:
        return [r for rules in RULES.values() for r in rules]

    def analyse_all(self) -> dict[str, dict]:
        out: dict[str, dict] = {}
        for name in self.assessments:
            counts = self.get_rule_counts(name)
            out[name] = {
                "total_violations": len(self.assessments[name]),
                "unique_rules": len(counts),
                "violations": dict(counts),
            }
        return out

    def generate_report(self, results: dict[str, dict]) -> str:
        detected = self.all_rules_detected()
        known = self.all_known_rules()
        not_triggered = [r for r in known if r not in detected]
        unknown = [r for r in detected if r not in known]

        lines: list[str] = ["# Zita Analysis Report", ""]
        lines += ["## Summary", ""]
        lines += [f"- **Submissions analysed:** {len(results)}"]
        lines += [f"- **Unique rules triggered:** {len(detected)}", ""]
        for name, data in sorted(results.items()):
            lines += [f"- {name} - Violations: {data['total_violations']} - Unique Rules: {data['unique_rules']}"]
        lines += [""]

        lines += ["## Rules Overview", ""]
        if not_triggered:
            lines += [f"### Rules Not Triggered ({len(not_triggered)})", ""]
            lines += [f"- {r}" for r in sorted(not_triggered)] + [""]
        if unknown:
            lines += [f"### Rules Not in Category List ({len(unknown)})", ""]
            for r in sorted(unknown):
                total = sum(d["violations"].get(r, 0) for d in results.values())
                lines += [f"- {r} ({total} total)"]
            lines += [""]

        lines += ["## Rules by Category", ""]
        for category, rules in RULES.items():
            hit = [
                (r, sum(d["violations"].get(r, 0) for d in results.values()))
                for r in rules
                if r in detected
            ]
            if hit:
                lines += [f"### {category.replace('_', ' ').title()}", ""]
                for r, total in sorted(hit, key=lambda x: x[1], reverse=True):
                    lines += [f"- {r} - Total: {total}"]
                lines += [""]

        lines += ["## Per-Submission Breakdown", ""]
        for name, data in sorted(results.items()):
            lines += [f"### {name}", ""]
            lines += [f"**Total:** {data['total_violations']} violations, {data['unique_rules']} unique rules", ""]
            for r, c in sorted(data["violations"].items(), key=lambda x: x[1], reverse=True):
                lines += [f"- {r} - Count: {c}"]
            lines += [""]
        return "\n".join(lines)


def main() -> None:
    if len(sys.argv) != 2:
        print(f"Usage: python {Path(__file__).name} <dir-of-csv-files>")
        sys.exit(2)

    analysis_dir = Path(sys.argv[1])
    if not analysis_dir.exists():
        print(f"Error: {analysis_dir} not found")
        sys.exit(1)

    csv_files = [
        f for f in analysis_dir.glob("*.csv")
        if "comparison" not in f.name.lower() and "analysis" not in f.name.lower()
    ]
    if not csv_files:
        print(f"Error: No CSV files in {analysis_dir}")
        sys.exit(1)

    analyser = ZitaAnalyzer()
    print(f"Loading {len(csv_files)} files...")
    for f in csv_files:
        name = f.stem.replace("_results", "")
        print(f"  {'+' if analyser.load_csv(f, name) else '-'} {name}")

    results = analyser.analyse_all()
    report = analyser.generate_report(results)

    (analysis_dir / "analysis_report.md").write_text(report, encoding="utf-8")
    (analysis_dir / "analysis_report.json").write_text(
        json.dumps(results, indent=2), encoding="utf-8"
    )

    detected = analyser.all_rules_detected()
    known = analyser.all_known_rules()
    not_triggered = [r for r in known if r not in detected]
    print(f"\nRules: {len(detected)} triggered, {len(not_triggered)} not triggered")
    print(f"Saved: {analysis_dir / 'analysis_report.md'}")


if __name__ == "__main__":
    main()
