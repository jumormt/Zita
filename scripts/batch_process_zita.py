#!/usr/bin/env python3
"""
Zita batch processor.

Walks a submissions directory and runs Zita over every sub-directory that
contains at least one .pde file. Writes one CSV, one JSON, and one student
feedback file per submission, plus a top-level batch_summary.json.

Usage (from repo root)::

    # All defaults (target/Zita.jar, src/main/resources/rulesets/rules.xml)
    python scripts/batch_process_zita.py path/to/submissions

    # Custom output dir
    python scripts/batch_process_zita.py path/to/submissions path/to/output

    # Full positional form
    python scripts/batch_process_zita.py target/Zita.jar src/main/resources/rulesets/rules.xml \
        path/to/submissions path/to/output

Submission convention: each submission is a directory containing at least
one .pde file. By Processing convention the directory name should match
the main .pde filename, but Zita only requires the .pde to be present.
"""

import json
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
# Defaults: prefer env vars (set by the Docker image), then repo-relative paths.
DEFAULT_JAR = Path(os.environ.get("ZITA_JAR", REPO_ROOT / "target" / "Zita.jar"))
DEFAULT_RULES = Path(
    os.environ.get("ZITA_RULES", REPO_ROOT / "src" / "main" / "resources" / "rulesets" / "rules.xml")
)


class ZitaBatchProcessor:
    def __init__(self, zita_jar: Path, rules: Path, submissions_dir: Path, output_dir: Path):
        self.zita_jar = Path(zita_jar)
        self.rules = Path(rules)
        self.submissions_dir = Path(submissions_dir)
        self.output_dir = Path(output_dir)

        for p, name in [
            (self.zita_jar, "Zita JAR"),
            (self.rules, "Rules XML"),
            (self.submissions_dir, "Submissions dir"),
        ]:
            if not p.exists():
                raise FileNotFoundError(f"{name} not found: {p}")

        self.csv_dir = self.output_dir / "csv"
        self.json_dir = self.output_dir / "json"
        self.student_dir = self.output_dir / "student_feedback"
        self.logs_dir = self.output_dir / "logs"
        for d in [self.csv_dir, self.json_dir, self.student_dir, self.logs_dir]:
            d.mkdir(parents=True, exist_ok=True)

    def find_submissions(self) -> list[Path]:
        return sorted(
            d
            for d in self.submissions_dir.iterdir()
            if d.is_dir() and any(d.glob("*.pde"))
        )

    def run_zita(self, submission: Path, output: Path, renderer: str) -> bool:
        cmd = [
            "java", "-jar", str(self.zita_jar),
            "--project", str(submission),
            "--rules", str(self.rules),
            "--renderer", renderer,
        ]
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=60,
                encoding="utf-8",
                errors="replace",
            )
            output.write_text(result.stdout, encoding="utf-8")
            return result.returncode == 0
        except (subprocess.TimeoutExpired, OSError):
            return False

    def process(self, submission: Path) -> tuple[bool, dict]:
        name = submission.name
        results = {
            "csv": self.run_zita(submission, self.csv_dir / f"{name}_results.csv", "csv"),
            "json": self.run_zita(submission, self.json_dir / f"{name}_results.json", "json"),
            "student": self.run_zita(submission, self.student_dir / f"{name}_feedback.txt", "student"),
        }
        return all(results.values()), results

    def process_all(self) -> None:
        submissions = self.find_submissions()
        if not submissions:
            print(f"No submissions found in {self.submissions_dir}")
            return

        print(f"Processing {len(submissions)} submissions...")
        results: dict[str, dict] = {}
        success = 0
        for i, sub in enumerate(submissions, 1):
            ok, res = self.process(sub)
            results[sub.name] = res
            success += ok
            print(f"  [{i}/{len(submissions)}] {'ok' if ok else 'WARN'} {sub.name}")

        summary = {
            "timestamp": datetime.now().isoformat(),
            "total": len(submissions),
            "successful": success,
            "failed": len(submissions) - success,
            "submissions": results,
        }
        (self.output_dir / "batch_summary.json").write_text(
            json.dumps(summary, indent=2), encoding="utf-8"
        )

        print(f"\nDone: {success}/{len(submissions)} successful")
        print(f"Output: {self.output_dir}")
        print(f"\nNext: python scripts/analyze_zita_results.py {self.csv_dir}")


def parse_args(argv: list[str]) -> tuple[Path, Path, Path, Path]:
    """Accept 1-arg, 2-arg, or 4-arg invocations."""
    if len(argv) == 1:
        submissions = Path(argv[0])
        return DEFAULT_JAR, DEFAULT_RULES, submissions, submissions / "batch-analysis"
    if len(argv) == 2:
        submissions, output = Path(argv[0]), Path(argv[1])
        return DEFAULT_JAR, DEFAULT_RULES, submissions, output
    if len(argv) == 4:
        return Path(argv[0]), Path(argv[1]), Path(argv[2]), Path(argv[3])
    print(f"Usage: python {Path(__file__).name} <submissions_dir> [output_dir]")
    print(f"   or: python {Path(__file__).name} <zita_jar> <rules_xml> <submissions_dir> <output_dir>")
    sys.exit(2)


def main() -> None:
    jar, rules, submissions, output = parse_args(sys.argv[1:])
    try:
        ZitaBatchProcessor(jar, rules, submissions, output).process_all()
    except FileNotFoundError as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
