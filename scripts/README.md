# Zita batch analysis scripts

Two Python scripts for running Zita over many Processing submissions and producing a comparison report. Originally authored by Tom Beggs (May 2026 handover); ported to Linux + Zita's actual CLI flags.

No third-party Python dependencies. Requires Python 3.10+ (for `dict[str, list]` syntax) and a built `target/Zita.jar`.

## Submission convention

A *submission* is a directory containing at least one `.pde` file. By Processing convention the directory name should match the main `.pde` filename (Processing IDE enforces this); Zita itself only looks for `.pde` files inside the directory.

```
submissions/
├── student-001/
│   ├── student-001.pde
│   └── Helper.pde
├── student-002/
│   └── student-002.pde
└── ...
```

## End-to-end usage

From the repository root:

```bash
# 1. Build Zita (only needed when source changes)
mvn -B clean package

# 2. Run Zita over every submission. Outputs CSV+JSON+student-feedback per
#    submission plus a top-level batch_summary.json.
python scripts/batch_process_zita.py path/to/submissions

# 3. Aggregate into one report
python scripts/analyze_zita_results.py path/to/submissions/batch-analysis/csv

# 4. Read the report
cat path/to/submissions/batch-analysis/csv/analysis_report.md
```

The default JAR and rules paths assume invocation from the repo root; see the `--help`-style usage strings in each script for full positional forms.

## Output layout

```
<output_dir>/
├── csv/
│   └── <submission>_results.csv          # PMD CSV renderer
├── json/
│   └── <submission>_results.json         # PMD JSON renderer
├── student_feedback/
│   └── <submission>_feedback.txt         # Zita's "student" renderer
├── logs/
└── batch_summary.json                    # success/fail counts per submission
```

After step 3 the CSV directory also contains `analysis_report.md` and `analysis_report.json`.

## Adding a new rule

When you register a new rule in `src/main/resources/rulesets/rules.xml`, also add its name to the appropriate category in the `RULES` dict at the top of `analyze_zita_results.py`. Rules outside this dict still get counted, but they appear under "Rules Not in Category List" instead of being grouped by topic in the categorised report.

## Differences from the handover originals

- Both scripts now default to repo-relative paths (`target/Zita.jar`, `src/main/resources/rulesets/rules.xml`) so they run cleanly from the repo root.
- `batch_process_zita.py` accepts 1-arg / 2-arg / 4-arg invocations rather than only 4-arg with a hardcoded `Internship/Projects/ZITA/` Windows layout.
- `analyze_zita_results.py` `RULES` table extended with the `ai_detection` category for the four AI-codegen rules added in the same handover.
- Stripped Unicode `✓`/`⚠` icons from console output (printed cleanly on more terminals).
