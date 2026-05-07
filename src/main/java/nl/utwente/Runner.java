package nl.utwente;

import net.sourceforge.pmd.renderers.*;
import nl.utwente.processing.pmd.rules.DoesItBuildRule;
import nl.utwente.renderers.AtelierStyleTextRenderer;
import nl.utwente.processing.ProcessingFile;
import nl.utwente.processing.ProcessingProject;
import nl.utwente.processing.pmd.PMDException;
import nl.utwente.processing.pmd.PMDRunner;
import nl.utwente.renderers.StudentFeedbackRenderer;
import nl.utwente.renderers.VivaHandoverRenderer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Runner {
    
    static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            // Print the error message to the screen
            System.out.println("Error reading file: " + path);
            System.out.println("Exception thrown reading files.");
            return "";
        }
    }

    // ANSI colors are emitted only on an interactive terminal, and disabled if NO_COLOR is set
    // (https://no-color.org/). Set FORCE_COLOR=1 to force-enable when stdout is piped.
    private static final boolean USE_COLOR = computeUseColor();

    private static boolean computeUseColor() {
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null && !noColor.isEmpty()) return false;
        String forceColor = System.getenv("FORCE_COLOR");
        if (forceColor != null && !forceColor.isEmpty()) return true;
        return System.console() != null;
    }

    private static final String RESET   = USE_COLOR ? "[0m"  : "";
    private static final String BOLD    = USE_COLOR ? "[1m"  : "";
    private static final String DIM     = USE_COLOR ? "[2m"  : "";
    private static final String CYAN    = USE_COLOR ? "[36m" : "";
    private static final String GREEN   = USE_COLOR ? "[32m" : "";
    private static final String YELLOW  = USE_COLOR ? "[33m" : "";
    private static final String MAGENTA = USE_COLOR ? "[35m" : "";

    private static String head(String s)   { return BOLD + CYAN + s + RESET; }      // section headers
    private static String flag(String s)   { return BOLD + GREEN + s + RESET; }     // --flag names
    private static String val(String s)    { return YELLOW + s + RESET; }           // values / defaults
    private static String title(String s)  { return BOLD + MAGENTA + s + RESET; }   // title line
    private static String comment(String s){ return DIM + s + RESET; }              // example comments

    static void printUsage() {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(title("Zita")).append(" — static analysis for Processing (.pde) sketches").append(nl);
        sb.append(nl);
        sb.append(head("USAGE")).append(nl);
        sb.append("  java -jar Zita.jar ").append(flag("--project")).append(" <path> ").append(flag("--rules")).append(" <path> [").append(flag("--renderer")).append(" <type>]").append(nl);
        sb.append("  java -jar Zita.jar ").append(flag("--help")).append(nl);
        sb.append(nl);
        sb.append(head("REQUIRED")).append(nl);
        sb.append("  ").append(flag("--project")).append(" <path>   Directory to analyze. All `.pde` files found recursively (up to").append(nl);
        sb.append("                     depth 10000) are concatenated into a single synthetic Java unit").append(nl);
        sb.append("                     before PMD runs. Pass the sketch root, not an individual `.pde`").append(nl);
        sb.append("                     file. Multi-file Processing projects work without extra setup.").append(nl);
        sb.append(nl);
        sb.append("  ").append(flag("--rules")).append(" <path>     PMD ruleset XML. The bundled default lives at").append(nl);
        sb.append("                     `src/main/resources/rulesets/rules.xml` in this repo, and at").append(nl);
        sb.append("                     `/app/rules.xml` inside the production Docker image. Custom").append(nl);
        sb.append("                     rulesets must declare each rule's fully-qualified class name and").append(nl);
        sb.append("                     `<priority>`; rules surfaced by the `student`/`handover`").append(nl);
        sb.append("                     renderers also need a `category` property and an entry in").append(nl);
        sb.append("                     `rule-category-mapping.properties`.").append(nl);
        sb.append(nl);
        sb.append(head("OPTIONAL")).append(nl);
        sb.append("  ").append(flag("--renderer")).append(" <type>  Output format. Default: ").append(val("zita")).append(". Accepted values:").append(nl);
        sb.append("                       ").append(val("zita")).append("      Atelier-style human-readable text (default)").append(nl);
        sb.append("                       ").append(val("student")).append("   Student-facing categorized feedback").append(nl);
        sb.append("                       ").append(val("handover")).append("  Viva/oral exam handover, grouped by category").append(nl);
        sb.append("                       ").append(val("html")).append("      PMD's standard HTML report").append(nl);
        sb.append("                       ").append(val("json")).append("      PMD's standard JSON report").append(nl);
        sb.append("                       ").append(val("csv")).append("       PMD's standard CSV report").append(nl);
        sb.append("                     Unknown values silently fall back to ").append(val("zita")).append(". The first three are").append(nl);
        sb.append("                     project-specific and require ruleset metadata (category property);").append(nl);
        sb.append("                     the latter three are stock PMD renderers.").append(nl);
        sb.append(nl);
        sb.append("  ").append(flag("--help")).append(", ").append(flag("-h")).append("         Print this message and exit 0.").append(nl);
        sb.append(nl);
        sb.append(head("OUTPUT")).append(nl);
        sb.append("  Reports are written to stdout. Redirect to a file with `> report.html` etc.").append(nl);
        sb.append("  Reported line numbers are mapped back from the concatenated Java unit to the").append(nl);
        sb.append("  originating `.pde` file when the renderer supports it (`zita`, `student`,").append(nl);
        sb.append("  `handover`); stock PMD renderers (`html`/`json`/`csv`) report against the").append(nl);
        sb.append("  synthesized `Processing.pde` file name.").append(nl);
        sb.append(nl);
        sb.append(head("EXAMPLES")).append(nl);
        sb.append("  ").append(comment("# Default text feedback against a bundled example")).append(nl);
        sb.append("  java -jar Zita.jar ").append(flag("--project")).append(" examples/01-bouncing-ball \\").append(nl);
        sb.append("      ").append(flag("--rules")).append(" src/main/resources/rulesets/rules.xml").append(nl);
        sb.append(nl);
        sb.append("  ").append(comment("# Student feedback renderer")).append(nl);
        sb.append("  java -jar Zita.jar ").append(flag("--project")).append(" ./submission ").append(flag("--rules")).append(" ./rules.xml ").append(flag("--renderer")).append(" ").append(val("student")).append(nl);
        sb.append(nl);
        sb.append("  ").append(comment("# JSON for downstream tooling")).append(nl);
        sb.append("  java -jar Zita.jar ").append(flag("--project")).append(" ./submission ").append(flag("--rules")).append(" ./rules.xml ").append(flag("--renderer")).append(" ").append(val("json")).append(" \\").append(nl);
        sb.append("      > report.json").append(nl);
        sb.append(nl);
        sb.append(head("NOTES")).append(nl);
        sb.append("  * `DoesItBuildRule` shells out to the external `processing-java` binary. If it is").append(nl);
        sb.append("    not on PATH, every project receives a single \"failed to build\" violation and").append(nl);
        sb.append("    the remaining rules still run normally.").append(nl);
        sb.append("  * Exit code is 0 whether or not violations are found; this tool does not gate").append(nl);
        sb.append("    builds. Parse the chosen renderer's output to decide pass/fail.").append(nl);
        sb.append("  * For batch processing across many submissions, see `scripts/batch_process_zita.py`").append(nl);
        sb.append("    and `docs/DOCKER.md`.").append(nl);
        sb.append(nl);
        sb.append(comment("(Set NO_COLOR=1 to disable colors, FORCE_COLOR=1 to force them in piped output.)")).append(nl);
        System.out.print(sb);
    }

    public static void main(String[] args) throws IOException, PMDException {

        String projectPath = null;
        String rulePath = null;
        String rendererType = "zita";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--project":
                    if (i + 1 < args.length)
                        projectPath = args[++i];
                    break;
                case "--rules":
                    if (i + 1 < args.length)
                        rulePath = args[++i];
                    break;
                case "--renderer":
                    if (i + 1 < args.length)
                        rendererType = args[++i];
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
            }
        }
        if (projectPath == null || rulePath == null) {
            printUsage();
            return;
        }

        var path = Path.of(projectPath);
        var rulePathStr = Path.of(rulePath).toString();

        DoesItBuildRule.Companion.setSketchPath(projectPath);

        var project = new ProcessingProject(
                Files.find(path, 10000,
                        (p, attr) -> attr.isRegularFile() && p.getFileName().toString().endsWith(".pde"))
                        .map(p -> new ProcessingFile(p.getFileName().toString(), p.getFileName().toString(),
                                readString(p)))
                        .collect(Collectors.toList()));

        var runner = new PMDRunner(rulePathStr);
        AbstractIncrementingRenderer renderer = null;
        AbstractAccumulatingRenderer accRenderer = null;
        switch (rendererType.toLowerCase()) {
            case "html":
                renderer = new HTMLRenderer(); 
                break;
            case "json":
                renderer = new JsonRenderer(); 
                break;
            
            case "csv":
            renderer = new CSVRenderer();
            break;

            case "handover":
                accRenderer = new VivaHandoverRenderer();
                break;

            case "student":
                accRenderer = new StudentFeedbackRenderer(project);
                break;
            default:
            renderer = new AtelierStyleTextRenderer(project);
        }

        if (accRenderer instanceof StudentFeedbackRenderer) {
            ((StudentFeedbackRenderer) accRenderer).setRuleSets(runner.getRuleSets());
        }
        if (accRenderer instanceof VivaHandoverRenderer) {
            ((VivaHandoverRenderer) accRenderer).setRuleSets(runner.getRuleSets());
        }
        if (accRenderer != null) {
            accRenderer.setWriter(new PrintWriter(System.out));
            runner.Run(project, accRenderer);
            DoesItBuildRule.Companion.resetRunFlag();
        } else {

           renderer.setWriter(new PrintWriter(System.out));
           runner.Run(project, renderer);
        }


        }
        }


