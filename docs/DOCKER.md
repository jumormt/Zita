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

### Run a bundled example sketch

The image ships three tiny example sketches at `/app/examples/` so you can verify it works without mounting anything:

```bash
docker run --rm zita \
  --project /app/examples/02-violations \
  --rules /app/rules.xml \
  --renderer zita
```

Try `01-bouncing-ball` (clean reference sketch) and `03-multi-file` (multi-file project) for contrast. See [`examples/README.md`](../examples/README.md) for what each sketch demonstrates.

### Run a single sketch from the host

```bash
docker run --rm \
  -v "$PWD/path/to/sketch:/work" \
  zita \
  --project /work --rules /app/rules.xml --renderer zita
```

Renderer values: `zita` (default), `student`, `handover`, `html`, `json`, `csv`. See `docs/TESTING.md` for the full walkthrough.

### Run the batch pipeline

The container's default entrypoint is `java -jar /app/Zita.jar`. For batch mode override the entrypoint to `python3`.

`batch_process_zita.py` requires a **flat** submissions layout — `submissions_dir` must contain one subdirectory per sketch, each holding the `.pde` file directly:

```text
submissions/
├── student-1/
│   └── student-1.pde
├── student-2/
│   └── student-2.pde
└── ...
```

Subdirectories without a top-level `.pde` are skipped silently (you'll see `No submissions found in /work` and exit 0). The script does not recurse into deeper trees — point `/work` at the level whose immediate children are the sketch directories.

> **Pre-create the output directory.** When you bind-mount a host path that doesn't exist yet, the Docker daemon (running as root) creates it as root *before* the container starts. The container's `--user "$(id -u):$(id -g)"` then can't write into it and the script crashes with `PermissionError: '/out/csv'`. Run `mkdir -p out` on the host first so the directory exists owned by you.

```bash
mkdir -p out

docker run --rm --user "$(id -u):$(id -g)" --entrypoint python3 \
  -v "$PWD/submissions:/work" \
  -v "$PWD/out:/out" \
  zita \
  /app/scripts/batch_process_zita.py /work /out
```

`--user "$(id -u):$(id -g)"` ensures the *files written inside* `out/` are owned by you (without it they'd be root-owned and need `sudo` to delete). It does **not** change ownership of the bind-mount target itself — that's why the `mkdir -p out` step above is required.

To try the batch pipeline against the bundled examples without supplying your own corpus, point `/work` at `$PWD/examples`:

```bash
mkdir -p out
docker run --rm --user "$(id -u):$(id -g)" --entrypoint python3 \
  -v "$PWD/examples:/work" -v "$PWD/out:/out" \
  zita /app/scripts/batch_process_zita.py /work /out
# Done: 3/3 successful
```

Then aggregate the results into a markdown report:

```bash
docker run --rm --user "$(id -u):$(id -g)" --entrypoint python3 \
  -v "$PWD/out:/out" \
  zita \
  /app/scripts/analyze_zita_results.py /out/csv
```

The report lands at `out/csv/analysis_report.md`.

### Use a custom ruleset

The image bundles `src/main/resources/rulesets/rules.xml` at `/app/rules.xml`. To override, mount a different file:

```bash
docker run --rm \
  -v "$PWD/path/to/sketch:/work" \
  -v "$PWD/my-rules.xml:/rules.xml:ro" \
  zita \
  --project /work --rules /rules.xml --renderer zita
```

### Interactive use

```bash
docker run --rm -it --user "$(id -u):$(id -g)" \
  -v "$PWD":"$PWD" -w "$PWD" --entrypoint bash zita

# inside the container — single sketch:
java -jar /app/Zita.jar --project examples/01-bouncing-ball \
  --rules src/main/resources/rulesets/rules.xml --renderer zita

# inside the container — batch + aggregate:
mkdir -p out
python3 /app/scripts/batch_process_zita.py examples out
python3 /app/scripts/analyze_zita_results.py out/csv
# Report at out/csv/analysis_report.md, all files owned by you on the host.
```

Passing `--user "$(id -u):$(id -g)"` keeps everything written under `$PWD/out/` host-user-owned, so no `docker run alpine rm -rf` cleanup is needed afterwards.

For purely interactive single-sketch work the simplest path is still the host JAR (`java -jar target/Zita.jar ...`). The image's real value is reproducible batch processing and deployment.

### What's intentionally NOT in the image

- **`processing-java`** (Processing IDE's CLI). It's required only by `DoesItBuildRule`, which gracefully degrades to a single "failed to build" violation per submission. Bundling Processing IDE would add hundreds of megabytes plus an X server dependency. Other rules are unaffected.
- **Maven, JDK source compiler.** The runtime image uses `eclipse-temurin:17-jre`, not `-jdk` — the JAR is shaded, no further compilation happens at runtime.

### Image layout

```text
/app/
├── Zita.jar              # the shaded uber-jar
├── rules.xml             # default ruleset (same as src/main/resources/rulesets/)
├── examples/             # three bundled demo sketches; safe to use as fixtures
│   ├── 01-bouncing-ball/
│   ├── 02-violations/
│   └── 03-multi-file/
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

- Microsoft's `mcr.microsoft.com/devcontainers/java:3-17-bookworm` base image:
  - JDK 17 (Eclipse Temurin) on Debian 12 (bookworm)
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

# Quick smoke against a bundled example
java -jar target/Zita.jar \
  --project examples/01-bouncing-ball \
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
| `PermissionError: '/out/csv'` from the batch script | The host `out/` directory didn't exist before `docker run`, so the daemon auto-created it as root and the in-container `--user` can't write into it. Run `mkdir -p out` on the host first. See "Run the batch pipeline" above. |
| `out/` (or other host-mounted dir) is root-owned and you can't `rm -rf` it without `sudo` | Use Docker itself as the privileged remover — no host sudo required: `docker run --rm -v "$PWD":/work alpine rm -rf /work/out`. The same trick fixes leftover `submissions/` or any other root-owned mount target. |
| `docker build` fails with Maven download errors | Network unreachable from inside Docker. If you're on a corporate proxy, set `--build-arg HTTP_PROXY=...` and update the Dockerfile to forward those into the Maven invocation. |
| Dev container fails on first build | Inspect output via "Dev Containers: Show Container Log". Most common cause is a network proxy the base image can't see; same fix as above. |
| Image is larger than 299 MB after a rebuild | Likely an updated base image — `eclipse-temurin:17-jre-jammy` and `maven:3.9-eclipse-temurin-17` are pinned to floating tags. Pin to a digest with `@sha256:...` if reproducibility matters. |
