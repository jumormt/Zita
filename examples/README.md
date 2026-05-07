# Examples

Small, self-contained Processing sketches for trying Zita without needing real
student work. Each subdirectory follows Processing's convention (folder name
matches the main `.pde` file name) so they also open directly in the Processing
IDE.

| Sketch | Purpose |
|--------|---------|
| [`01-bouncing-ball/`](01-bouncing-ball) | Reference "clean" sketch — exercises a user-defined class with constructor, arithmetic, loops, conditionals, boolean operators, an event handler, an array, a non-void function, and a parameterised method. Most `minimum.*` rules should pass on this. |
| [`02-violations/`](02-violations) | Deliberate anti-patterns — short variable names (`x`, `y`), drawing in `setup`, `#RRGGBB` color literal, no class, no event handler. Useful for seeing rule output up front. |
| [`03-multi-file/`](03-multi-file) | Two-file sketch (`03-multi-file.pde` + `Particle.pde`). Demonstrates Zita's project-level concatenation: a class declared in one file is visible from another without imports. |

## Running

From the repo root, after building (`mvn clean package`):

```sh
java -jar target/Zita.jar \
  --project examples/01-bouncing-ball \
  --rules src/main/resources/rulesets/rules.xml \
  --renderer zita
```

Swap `--renderer` between `zita`, `student`, `handover`, `html`, `json`, `csv`
to compare output formats. The `01-bouncing-ball` sketch should report few
violations; `02-violations` should report many.

## Note on `processing-java`

`DoesItBuildRule` shells out to the `processing-java` CLI. If it isn't on
`PATH`, you'll see a one-line warning in the output (`Cannot run program
"processing-java"`); rule analysis itself still completes. Install Processing
and add `processing-java` to `PATH` if you want that rule to actually try to
build the sketch.
