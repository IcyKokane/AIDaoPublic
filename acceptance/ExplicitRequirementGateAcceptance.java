package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Proves explicit prompt requirements can block APK-ready status when generated source drops them. */
public final class ExplicitRequirementGateAcceptance {
    public static void main(String[] args) {
        GeneratedProject note = noteProject();
        assertNoFailure(note, "baseline notepad");
        assertRejected(withMutation(note, "/AppScreen.java",
                "root.addView(nav,new LinearLayout.LayoutParams(dp(104),-1))",
                "root.addView(nav,new LinearLayout.LayoutParams(-1,-2))"),
                "sidebar", "missing material sidebar layout");
        assertRejected(withoutSuffix(note, "/ic_generated_app.xml"),
                "app-logo", "missing requested generated logo artwork");
        assertRejected(withoutSuffix(note, "/EditorActivity.java"),
                "note-lock", "missing requested note lock/editor contract");
        assertRejected(withMutation(note, "app/src/main/res/values/strings.xml", "NoteForge",
                "Create A Notepad App That Uses A Sidebar To Navigate Different Screens"),
                "identity", "raw prompt reused as product identity");

        GeneratedProject workout = workoutProject();
        assertNoFailure(workout, "baseline workout");
        assertRejected(withMutation(workout, "/TimelineActivity.java",
                "store.number(\"workout_xp\",store.number(\"workout_xp\")+gain)",
                "store.number(\"workout_xp\",store.number(\"workout_xp\"))"),
                "RPG growth", "automatic workout progression mutation removed");

        System.out.println("PASS explicit requirement negative release-gate regressions");
    }

    private static GeneratedProject noteProject() {
        return new LocalSourceGenerator().generate(
                "Create A Notepad App That Uses A Sidebar To Navigate Different Screens",
                "Create a notepad app that uses a sidebar to navigate different screens. It should allow me to lock notes so they can't be edited. It should also have an app logo. It should use a modern UI that is purple and red.",
                Arrays.asList("Use a sidebar", "Lock notes", "Generate app logo", "Purple and red modern UI"),
                Arrays.asList("Generate", "Validate"));
    }

    private static GeneratedProject workoutProject() {
        return new LocalSourceGenerator().generate(
                "Simple Workout Tracker",
                "Create a simple workout tracking app. Track exercise, weight and reps. Use an RPG type UI and automatically show growth as RPG stats; stats are calculated by completed workouts, not manually entered.",
                Arrays.asList("Track exercise, weight, reps", "Automatically calculate RPG growth"),
                Arrays.asList("Generate", "Validate"));
    }

    private static GeneratedProject withMutation(GeneratedProject source, String suffix, String from, String to) {
        List<GeneratedProject.FileEntry> files = new ArrayList<>();
        boolean changed = false;
        for (GeneratedProject.FileEntry f : source.files) {
            String content = f.content;
            if (!changed && f.path != null && (f.path.equals(suffix) || f.path.endsWith(suffix)) && content.contains(from)) {
                content = content.replace(from, to);
                changed = true;
            }
            files.add(new GeneratedProject.FileEntry(f.path, content, f.taskHint));
        }
        if (!changed) throw new IllegalStateException("Regression fixture could not mutate " + suffix + " token " + from);
        return GeneratedProject.resolved(source.projectName, source.packageName, files, new ArrayList<>());
    }

    private static GeneratedProject withoutSuffix(GeneratedProject source, String suffix) {
        List<GeneratedProject.FileEntry> files = new ArrayList<>();
        boolean removed = false;
        for (GeneratedProject.FileEntry f : source.files) {
            if (!removed && f.path != null && f.path.endsWith(suffix)) { removed = true; continue; }
            files.add(new GeneratedProject.FileEntry(f.path, f.content, f.taskHint));
        }
        if (!removed) throw new IllegalStateException("Regression fixture could not remove " + suffix);
        return GeneratedProject.resolved(source.projectName, source.packageName, files, new ArrayList<>());
    }

    private static void assertNoFailure(GeneratedProject project, String label) {
        for (String note : project.verificationNotes)
            if (note.startsWith("FAIL ")) throw new IllegalStateException(label + " unexpectedly failed: " + note);
    }

    private static void assertRejected(GeneratedProject project, String expectedText, String label) {
        for (String note : project.verificationNotes)
            if (note.startsWith("FAIL ") && note.toLowerCase().contains(expectedText.toLowerCase())) return;
        throw new IllegalStateException(label + " did not block readiness. Notes=" + project.verificationNotes);
    }
}
