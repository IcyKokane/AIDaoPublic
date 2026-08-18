package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** Writes the two real-device non-media regression projects so CI can compile their actual Android source. */
public final class GenerateGeneralFidelityProjects {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected notepad and workout output directories");

        GeneratedProject notepad = new LocalSourceGenerator().generate(
                "Create A Notepad App That Uses A Sidebar To Navigate Different Screens",
                "Create a notepad app that uses a sidebar to navigate different screens. It should allow me to lock notes so they can't be edited. It should also have an app logo. It should use a modern UI that is purple and red.",
                Arrays.asList(
                        "Use a sidebar to navigate between writing, editor, search, and library screens.",
                        "Allow notes to be locked so locked notes cannot be edited until explicitly unlocked.",
                        "Generate and wire a distinct launcher app logo.",
                        "Use a modern purple and red visual theme.",
                        "Persist saved notes and lock state locally."
                ),
                Arrays.asList("Generate identity", "Generate UI", "Generate note persistence", "Validate fidelity")
        );

        GeneratedProject workout = new LocalSourceGenerator().generate(
                "Simple Workout Tracker",
                "Create a simple workout tracking app. It should track exercise, weight and reps. It should have an RPG type of UI and automatically show growth in the form of RPG stats; growth should be calculated by the app rather than manually entered.",
                Arrays.asList(
                        "Track exercise name, weight, and reps locally.",
                        "Calculate progression automatically from completed workouts.",
                        "Display RPG-style stats and growth without manual stat input."
                ),
                Arrays.asList("Generate workout model", "Generate tracker UI", "Calculate progression", "Validate source")
        );

        write(Paths.get(args[0]), notepad, "notepad");
        write(Paths.get(args[1]), workout, "workout");
    }

    private static void write(Path rawOutput, GeneratedProject project, String label) throws Exception {
        Path output = rawOutput.toAbsolutePath().normalize();
        Files.createDirectories(output);
        for (String note : project.verificationNotes) {
            System.out.println(label + ": " + note);
            if (note.startsWith("FAIL ")) throw new IllegalStateException(label + " verification failed: " + note);
        }
        for (GeneratedProject.FileEntry entry : project.files) {
            Path target = output.resolve(entry.path).normalize();
            if (!target.startsWith(output)) throw new SecurityException("Generated path escaped output root: " + entry.path);
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.write(target, entry.content.getBytes(StandardCharsets.UTF_8));
        }
        if (!project.hasPath("app/src/main/AndroidManifest.xml")) throw new IllegalStateException(label + " missing manifest");
        if (!project.hasPath("app/build.gradle.kts")) throw new IllegalStateException(label + " missing app Gradle file");
        String javaRoot = "app/src/main/java/" + project.packageName.replace('.', '/') + "/";
        if (!project.hasPath(javaRoot + "MainActivity.java")) throw new IllegalStateException(label + " missing MainActivity");
        if (!project.hasPath(javaRoot + "AppScreen.java")) throw new IllegalStateException(label + " missing AppScreen");
        System.out.println("Generated " + label + " compile project: " + project.projectName + " / " + project.packageName + " / " + project.files.size() + " files");
    }

    private GenerateGeneralFidelityProjects() {}
}
