# Docker and Dev Container Guide

Two ways to run Zita without a host JDK / Maven / Python install:

1. **Production image** (`Dockerfile`) — multi-stage build that produces a 299 MB runtime image bundling the JAR, the default ruleset, and the Python batch scripts.
2. **Dev container** (`.devcontainer/`) — full development environment for VS Code Remote Containers and GitHub Codespaces, with JDK 17, Maven, Python 3.11, and the Java/Kotlin/Python/XML/Markdown extensions pre-installed.

---

## Production image

### Build

```bash
docker build -t zita .
# Layers cached after the first build — subsequent rebuilds finish in ~15s
# unless pom.xml or src/ changed.
```

### Run a single sketch

```bash
docker run --rm \
  -v "$PWD/MySketch:/work" \
  zita \
  --project /work --rules /app/rules.xml --renderer zita
```

Renderer values: `zita` (default), `student`, `handover`, `html`, `json`, `csv`. See `docs/TESTING.md` for the full walkthrough.

### Run the batch pipeline

The container's default entrypoint is `java -jar /app/Zita.jar`. For batch mode override the entrypoint to `python3`:

```bash
docker run --rm --entrypoint python3 \
  -v "$PWD/submissions:/work" \
  -v "$PWD/out:/out" \
  zita \
  /app/scripts/batch_process_zita.py /work /out
```

Then aggregate the results into a markdown report:

```bash
docker run --rm --entrypoint python3 \
  -v "$PWD/out:/out" \
  zita \
  /app/scripts/analyze_zita_results.py /out/csv
```

The report lands at `out/csv/analysis_report.md`.

### Use a custom ruleset

The image bundles `src/main/resources/rulesets/rules.xml` at `/app/rules.xml`. To override, mount a different file:

```bash
docker run --rm \
  -v "$PWD/MySketch:/work" \
  -v "$PWD/my-rules.xml:/rules.xml:ro" \
  zita \
  --project /work --rules /rules.xml --renderer zita
```

### What's intentionally NOT in the image

- **`processing-java`** (Processing IDE's CLI). It's required only by `DoesItBuildRule`, which gracefully degrades to a single "failed to build" violation per submission. Bundling Processing IDE would add hundreds of megabytes plus an X server dependency. Other rules are unaffected.
- **Maven, JDK source compiler.** The runtime image uses `eclipse-temurin:17-jre`, not `-jdk` — the JAR is shaded, no further compilation happens at runtime.

### Image layout

```
/app/
├── Zita.jar              # the shaded uber-jar
├── rules.xml             # default ruleset (same as src/main/resources/rulesets/)
└── scripts/
    ├── batch_process_zita.py
    ├── analyze_zita_results.py
    └── README.md
/workspace                # WORKDIR; mount your sketches/submissions here
```

Environment variables:

- `ZITA_JAR` (default `/app/Zita.jar`) — used by `batch_process_zita.py` when no JAR is passed positionally
- `ZITA_RULES` (default `/app/rules.xml`) — used by `batch_process_zita.py` when no ruleset is passed positionally

---

## Dev container

The repository ships with a `.devcontainer/` config that works in:

- **VS Code Dev Containers** — install the extension, then `F1` → "Dev Containers: Reopen in Container"
- **GitHub Codespaces** — works automatically when you open the repo in a codespace

### What you get

- Microsoft's `mcr.microsoft.com/devcontainers/java:1-17-jammy` base image:
  - JDK 17 (Eclipse Temurin)
  - Maven 3
  - Git and common Linux dev tooling
- Python 3.11 (via the `devcontainers/features/python:1` feature)
- VS Code extensions auto-installed:
  - Java extension pack (Microsoft)
  - Kotlin (`fwcd.kotlin`)
  - Python (Microsoft)
  - XML (Red Hat)
  - YAML (Red Hat)
  - markdownlint (David Anson)
- `mvn -B -q clean package -DskipTests` runs automatically on first attach so `target/Zita.jar` is ready immediately

### After the container starts

```bash
# Verify the build succeeded
ls -la target/Zita.jar

# Run the manual test walkthrough
cat docs/TESTING.md

# Quick smoke
java -jar target/Zita.jar \
  --project "temp/handover/ExamplePrograms/AssessmentB/Hopper" \
  --rules src/main/resources/rulesets/rules.xml \
  --renderer zita | head -10
```

### Customising

If you need extra tooling (e.g. `processing-java`, `kotlinc` standalone, additional Python packages), edit `.devcontainer/devcontainer.json` — either add to the `features` list (for things published as devcontainer features) or use `postCreateCommand` to run arbitrary install commands.

---

## Troubleshooting

| Symptom | Likely cause and fix |
|---------|----------------------|
| `Cannot run program "processing-java"` violation in container output | Expected. `processing-java` is not bundled. Other rules still run. |
| Permission errors writing to mounted `out/` directory | The container runs as `root` by default in the production image. Either `chown` the host directory afterwards or pass `--user "$(id -u):$(id -g)"` to `docker run`. |
| `docker build` fails with Maven download errors | Network unreachable from inside Docker. If you're on a corporate proxy, set `--build-arg HTTP_PROXY=...` and update the Dockerfile to forward those into the Maven invocation. |
| Dev container fails on first build | Inspect output via "Dev Containers: Show Container Log". Most common cause is a network proxy the base image can't see; same fix as above. |
| Image is larger than 299 MB after a rebuild | Likely an updated base image — `eclipse-temurin:17-jre-jammy` and `maven:3.9-eclipse-temurin-17` are pinned to floating tags. Pin to a digest with `@sha256:...` if reproducibility matters. |
