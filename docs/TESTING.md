# Manual Testing Guide

A hands-on walkthrough that validates Zita end-to-end: building the JAR, running the analyser on individual sketches, exercising every renderer, batch-processing a cohort, generating an aggregate report, and sanity-checking the AI-detection rules.

Each test states what you should expect to see. If your output differs, the **Troubleshooting** section at the bottom maps common symptoms to likely causes.

---

## Prerequisites

- Java 11+ (Java 17 is fine — CI uses 17)
- Maven 3 (Maven 3.9+ recommended; project has no `mvnw` wrapper)
- Python 3.10+ (only needed for the batch scripts)

> **Don't want to install any of these on the host?** See [`docs/DOCKER.md`](DOCKER.md) for the production image (everything pre-bundled) or the dev container config (full VS Code environment).

Confirm you are in the repo root and the JAR is built:

```bash
cd /path/to/Zita
ls -la target/Zita.jar
# Expect: ~8.2 MB shaded uber-jar.
# If it's missing or stale, build from source:
mvn -B clean package
```

All tests below run against sketches the repo ships in [`examples/`](../examples/) or that you generate inline as a temporary fixture. No additional corpus is required.

---

## Test 1 — Single sketch with the default renderer

Run the analyser against the bundled `02-violations` sketch — it deliberately trips a wide range of rules and is a good showcase of the default renderer's output:

```bash
java -jar target/Zita.jar \
  --project examples/02-violations \
  --rules src/main/resources/rulesets/rules.xml \
  --renderer zita
```

**Expected output** (excerpt):

```
> Event handlers should contain control flow or call declared methods
> Few or no 2D shapes found in the code.
> No arrays are used in this code.
> No classes were found. Try adding a class to organize your drawing code ...
> No user-defined class found.
> In file 02-violations.pde at line 1: Avoid variables with short names like x
> In file 02-violations.pde at line 6: Do not use drawing method background in method setup ...
```

Pass criterion: 20+ rule hits, including the "no class", "no array", and "drawing method in setup" findings. To verify AI-detection rules specifically, use Test 3 below — this sketch is hand-crafted as a generic anti-pattern showcase, not an AI-style submission.

---

## Test 2 — Student renderer

The `student` renderer groups violations into "Minimum Requirements" and "Demonstrates Mastery" sections, intended for student-facing feedback.

```bash
java -jar target/Zita.jar \
  --project examples/01-bouncing-ball \
  --rules src/main/resources/rulesets/rules.xml \
  --renderer student
```

**Expected output** (excerpt):

```
========================================
MINIMUM REQUIREMENTS
========================================

-- General --
> Has Header Comment Rule: ✅
> Variable Arithmetic Rule: ✅

-- Methods/functions --
> Has User Defined Method: ✅
...

-- Demonstrates-mastery --
> Useful Event Handler Rule: ✅
...
```

Pass criterion: output is grouped under category headings; per-rule status uses `✅` / `❌` markers.

---

## Test 3 — Hand-crafted AI-style sketch

Author a deliberately AI-flavoured sketch and verify each detection rule fires:

```bash
mkdir -p /tmp/zita-test/MySketch
cat > /tmp/zita-test/MySketch/MySketch.pde <<'EOF'
// ================ GLOBAL VARIABLES ================
// Author: [Your Name]
// Reference: https://en.wikipedia.org/wiki/Pong

int x = 100;
int dir = random(1) > 0.5 ? 1 : -1;

void setup() {
  size(400, 400);
}

void draw() {
  background(0);  // black background each frame
  fill(255);      // white fill
  ellipse(x, 200, 20, 20);  // draw circle
  if (random(1) < 0.01) dir *= -1;
  x += dir;
}
EOF

java -jar target/Zita.jar --project /tmp/zita-test/MySketch \
  --rules src/main/resources/rulesets/rules.xml --renderer zita 2>&1 \
  | grep -E "Has(Placeholder|Decorative|Wikipedia|RandomDirection|ExcessiveInline|FrameCount|EmptyMethodBody|Citation)|^>"
```

**Expected hits**:

| Rule | Why |
|------|-----|
| `HasDecorativeSectionCommentsRule` | The `================` divider |
| `HasPlaceholderAuthorRule` | `[Your Name]` |
| `HasWikipediaReferenceRule` | `wikipedia.org` URL |
| `HasRandomDirectionPatternRule` | `random(1) > 0.5 ? 1 : -1` |
| `HasRandomDirectionChangePatternRule` | `if (random(1) < 0.01) dir *= -1` |
| `HasExcessiveInlineDocumentationRule` | three short API-narrating comments |

If any of the six is missing, see Troubleshooting → "expected rule did not fire".

---

## Test 4 — Batch analysis on a cohort

Build a small cohort from the bundled examples plus the hand-crafted AI sketch from Test 3, then run the batch pipeline:

```bash
mkdir -p /tmp/my-cohort
cp -r examples/01-bouncing-ball  /tmp/my-cohort/
cp -r examples/02-violations     /tmp/my-cohort/
cp -r examples/03-multi-file     /tmp/my-cohort/
cp -r /tmp/zita-test/MySketch    /tmp/my-cohort/   # hand-crafted AI sketch from Test 3

ls /tmp/my-cohort
# Expect: 4 directories
```

Important: use `cp -r`, not `ln -s`. Zita's `Files.find` does not follow symlinks; symlinked submissions silently produce empty results.

```bash
python3 scripts/batch_process_zita.py /tmp/my-cohort /tmp/my-cohort/out
```

**Expected output**:

```
Processing 4 submissions...
  [1/4] ok 01-bouncing-ball
  [2/4] ok 02-violations
  [3/4] ok 03-multi-file
  [4/4] ok MySketch

Done: 4/4 successful
Output: /tmp/my-cohort/out
```

The output directory now contains:

```
/tmp/my-cohort/out/
├── csv/                       # PMD CSV (input for the analyser)
├── json/                      # PMD JSON (machine-readable)
├── student_feedback/          # Student renderer text
├── logs/
└── batch_summary.json         # success/fail counts
```

---

## Test 5 — Aggregate report

```bash
python3 scripts/analyze_zita_results.py /tmp/my-cohort/out/csv
cat /tmp/my-cohort/out/csv/analysis_report.md | head -30
```

**Expected output**:

```
# Zita Analysis Report

## Summary

- **Submissions analysed:** 4
- **Unique rules triggered:** N

- 01-bouncing-ball - Violations: ...
- 02-violations    - Violations: ...
- 03-multi-file    - Violations: ...
- MySketch         - Violations: ...
```

Per-submission counts will differ across submissions — `02-violations` should be highest, `01-bouncing-ball` lowest. If every row shows the same number, see Troubleshooting → "all submissions show identical counts".

---

## Test 6 — Inspect the AI-detection signals

```bash
grep -A 12 "^### Ai Detection" /tmp/my-cohort/out/csv/analysis_report.md
```

**Expected output** (counts will reflect the 4-submission cohort — most AI-detection rules fire only on the hand-crafted `MySketch`):

```
### Ai Detection

- HasDecorativeSectionCommentsRule - Total: 1
- HasPlaceholderAuthorRule - Total: 1
- HasWikipediaReferenceRule - Total: 1
- HasRandomDirectionPatternRule - Total: 1
- HasRandomDirectionChangePatternRule - Total: 1
- HasExcessiveInlineDocumentationRule - Total: 1
- HasFrameCountMagicNumberRule - Total: 0   (or omitted)
- HasEmptyMethodBodyRule - Total: 0         (or omitted)
- HasCitationCommentsRule - Total: 0        (or omitted)
```

A real cohort with multiple AI submissions would show higher totals on the first six rules; this small fixture cohort just confirms the signals are wired correctly.

---

## Test 7 — Per-submission breakdown

Drill into one submission's violations:

```bash
sed -n '/^### MySketch$/,/^### /p' /tmp/my-cohort/out/csv/analysis_report.md
```

**Expected output**: a `### MySketch` block listing every rule that fired on the hand-crafted sketch from Test 3, sorted by hit count.

---

## Test 8 — Negative control (false-positive check)

A deliberately "clean" sketch should not trigger AI-detection rules. The bundled `examples/01-bouncing-ball` is suitable, or use the inline `CleanSketch` below:

```bash
java -jar target/Zita.jar --project examples/01-bouncing-ball \
  --rules src/main/resources/rulesets/rules.xml --renderer zita 2>&1 \
  | grep -E "Has(Placeholder|Decorative|Wikipedia|RandomDirection|ExcessiveInline|FrameCount|EmptyMethodBody|Citation)" \
  | wc -l
# Expected: 0
```

Or, with a hand-crafted clean sketch:

```bash
mkdir -p /tmp/zita-test/CleanSketch
cat > /tmp/zita-test/CleanSketch/CleanSketch.pde <<'EOF'
// My drawing program for COMP1000 assignment
// Inspired by the bouncing ball example from week 4 lab

class Ball {
  float x, y, vx, vy;
  Ball(float startX, float startY) {
    x = startX;
    y = startY;
    vx = 2;
    vy = 3;
  }
  void update() {
    x += vx;
    y += vy;
    if (x < 0 || x > width)  vx = -vx;
    if (y < 0 || y > height) vy = -vy;
  }
  void display() {
    fill(0, 100, 255);
    ellipse(x, y, 30, 30);
  }
}

Ball myBall;

void setup() {
  size(600, 400);
  myBall = new Ball(width/2, height/2);
}

void draw() {
  background(240);
  myBall.update();
  myBall.display();
}
EOF

java -jar target/Zita.jar --project /tmp/zita-test/CleanSketch \
  --rules src/main/resources/rulesets/rules.xml --renderer zita 2>&1 \
  | grep -E "Has(Placeholder|Decorative|Wikipedia|RandomDirection|ExcessiveInline|FrameCount|EmptyMethodBody|Citation)" \
  | wc -l
```

**Expected output**: `0` (or at most `1`). Demonstrates the AI-detection rules do not fire on plausibly human-written code.

---

## Test 9 — Performance baseline

```bash
time java -jar target/Zita.jar \
  --project examples/02-violations \
  --rules src/main/resources/rulesets/rules.xml \
  --renderer zita > /dev/null
```

**Expected**: 2–4 seconds wall clock for a single small sketch on modern hardware. Larger sketches (multi-file projects with many classes) typically finish in 4–6 seconds, and batch sweeps scale roughly linearly with submission count.

---

## Cleanup

```bash
rm -rf /tmp/zita-test /tmp/my-cohort
```

---

## Troubleshooting

| Symptom | Likely cause and fix |
|---------|----------------------|
| `Cannot run program "processing-java"` violation appears in output | Expected. `DoesItBuildRule` shells out to Processing's CLI which is not installed. Other rules are unaffected. |
| `IllegalArgumentException: Cannot set non-existent property '<name>' on Rule <name>` | The on-disk `rules.xml` and the bundled one in `target/Zita.jar` have drifted. Run `mvn -B package` to rebuild and re-bundle. |
| Every batch submission reports identical counts (e.g. `25/25`) | The submissions directory uses symlinks. Zita's `Files.find` does not follow symlinks — use real `cp -r` copies instead. |
| A rule appears under "Rules Not in Category List" in the report | Add it to the appropriate category in `scripts/analyze_zita_results.py`'s `RULES` dict, then re-run the analyser (no Maven rebuild needed). |
| Build fails with `Expecting a top level declaration` in `DoesItBuildRule.kt` | The KDoc opener typo (`*/**` instead of `/**`) on `main`. Fix is in upstream PR `Addzyyy/Zita#10` and on the local research branch. |
| Build succeeds but `mvn package` followed by JAR run throws `IllegalArgumentException` mentioning `category` | The `category` `<property>` in `rules.xml` references a `PropertyDescriptor` not declared in the corresponding Kotlin rule class. Fix is in upstream PR `Addzyyy/Zita#11` and on the local research branch. |
| Expected rule did not fire in Test 3 | Confirm the sketch text matches verbatim — most AI-detection rules are pattern-based and may miss formatting variants. Run with `--renderer json` to see raw violations and inspect rule-class behaviour against the sketch source. |

---

## The three commands worth memorising

```bash
# A. Inspect a single sketch
java -jar target/Zita.jar --project <DIR> --rules src/main/resources/rulesets/rules.xml --renderer zita

# B. Batch run + aggregate report
python3 scripts/batch_process_zita.py <SUBMISSIONS_DIR>
python3 scripts/analyze_zita_results.py <SUBMISSIONS_DIR>/batch-analysis/csv

# C. View the AI fingerprint section of the report
grep -A 12 "^### Ai Detection" <SUBMISSIONS_DIR>/batch-analysis/csv/analysis_report.md
```
