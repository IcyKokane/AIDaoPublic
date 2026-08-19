package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Prevents request-fidelity home screens from emitting an unqualified Android
 * Button type without an import. This catches a generated-source compile failure
 * before the heavier Gradle regression builds.
 */
public final class GeneratedHomeButtonCompilationAcceptance {
    public static void main(String[] args) {
        GeneratedProject note = new LocalSourceGenerator().generate(
                "Create A Notepad App That Uses A Sidebar To Navigate Different Screens",
                "Create a notepad app that uses a sidebar to navigate different screens. It should allow me to lock notes so they can't be edited. It should also have an app logo. It should use a modern UI that is purple and red.",
                Arrays.asList("Use a sidebar", "Lock notes", "Generate app logo", "Purple and red modern UI"),
                Arrays.asList("Generate", "Validate"));
        GeneratedProject workout = new LocalSourceGenerator().generate(
                "Simple Workout Tracker",
                "Create a simple workout tracking app. Track exercise, weight and reps. Use an RPG type UI and automatically show growth as RPG stats; stats are calculated by completed workouts, not manually entered.",
                Arrays.asList("Track exercise, weight, reps", "Automatically calculate RPG growth"),
                Arrays.asList("Generate", "Validate"));

        assertButtonTypeResolvable(note, "notepad");
        assertButtonTypeResolvable(workout, "workout");
        System.out.println("PASS generated request-fidelity home Button type compilation contract");
    }

    private static void assertButtonTypeResolvable(GeneratedProject project, String label) {
        String suffix = "/MainActivity.java";
        GeneratedProject.FileEntry main = null;
        for (GeneratedProject.FileEntry f : project.files) {
            if (f != null && f.path != null && f.path.endsWith(suffix)) { main = f; break; }
        }
        if (main == null) throw new IllegalStateException(label + " generated no MainActivity");
        String src = main.content == null ? "" : main.content;
        if (!src.contains("Button ")) return;
        boolean imported = src.contains("import android.widget.Button;") || src.contains("import android.widget.*;");
        boolean qualified = src.contains("android.widget.Button ");
        if (!imported && !qualified) {
            throw new IllegalStateException(label + " MainActivity emits unresolved android.widget.Button type: " + src);
        }
    }
}
