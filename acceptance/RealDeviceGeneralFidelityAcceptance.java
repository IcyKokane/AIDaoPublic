package dev.thefoolish.aidao;

import java.util.Arrays;

/** Regression coverage derived from the first V1 real-device tests. */
public final class RealDeviceGeneralFidelityAcceptance {
    public static void main(String[] args) {
        verifyNotepadPrompt();
        verifyWorkoutPrompt();
        System.out.println("PASS real-device general fidelity regression");
    }

    private static void verifyNotepadPrompt() {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Create A Notepad App That Uses A Sidebar",
                "Create a notepad app that uses a sidebar to navigate different screens. It should allow me to lock notes so they can't be edited. It should also have an app logo. It should use a modern UI that is purple and red.",
                Arrays.asList(
                        "Use sidebar navigation.",
                        "Lock notes so locked notes cannot be edited.",
                        "Generate and wire an app logo.",
                        "Use a modern purple and red UI.",
                        "Persist notes and lock state across restart."
                ),
                Arrays.asList("Generate note UI", "Persist notes", "Implement locking", "Generate branding")
        );
        assertNoFail(project, "notepad");
        if (project.projectName.toLowerCase().startsWith("create ") || project.projectName.length() > 28)
            throw new IllegalStateException("notepad prompt leaked into app identity: " + project.projectName);
        String all = all(project).toLowerCase();
        String manifest = content(project, "app/src/main/AndroidManifest.xml").toLowerCase();
        if (!all.contains("sidenav") && !all.contains("linearlayout.vertical"))
            throw new IllegalStateException("sidebar navigation was not generated");
        if (!all.contains("#7c3aed") && !all.contains("0xff7c3aed"))
            throw new IllegalStateException("purple theme direction missing");
        if (!all.contains("#ef4444") && !all.contains("0xffef4444"))
            throw new IllegalStateException("red theme direction missing");
        if (!manifest.contains("android:icon=\"@drawable/ic_generated_app\""))
            throw new IllegalStateException("generated app logo is not wired into manifest");
        if (!all.contains("locked") || !(all.contains("setenabled(false)") || all.contains("read-only") || all.contains("read only")))
            throw new IllegalStateException("locked note read-only behavior missing");
        if (!all.contains("documents") || !all.contains("note_body_") || !all.contains("note_title_"))
            throw new IllegalStateException("restart-safe note persistence path missing");
    }

    private static void verifyWorkoutPrompt() {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Create A Simple Workout Tracking App",
                "Create a simple workout tracking app. It should track the exercise, weight and reps. It should have an RPG type UI and show growth as RPG stats. Workouts should be automatically in the app, not something that needs to be input.",
                Arrays.asList(
                        "Track exercise, weight, and reps.",
                        "Use an RPG-style progression UI.",
                        "Derive XP, level, and stats automatically from completed workouts.",
                        "Exercises are preloaded; the user should not type exercise names.",
                        "Persist workout history across restart."
                ),
                Arrays.asList("Generate tracker", "Generate preset exercises", "Persist workout history", "Calculate RPG progression")
        );
        assertNoFail(project, "workout");
        String all = all(project).toLowerCase();
        if (!all.contains("spinner") || !all.contains("squat") || !all.contains("bench press"))
            throw new IllegalStateException("workout exercise catalog is not built in");
        if (!all.contains("weight") || !all.contains("reps"))
            throw new IllegalStateException("workout measurement inputs missing");
        if (!all.contains("workout_xp") || !all.contains("level"))
            throw new IllegalStateException("automatic RPG progression missing");
        if (!all.contains("workout_history") || !(all.contains("puttext") || all.contains("sharedpreferences")))
            throw new IllegalStateException("workout history persistence missing");
    }

    private static void assertNoFail(GeneratedProject project, String label) {
        for (String note : project.verificationNotes)
            if (note.startsWith("FAIL ")) throw new IllegalStateException(label + " source generation blocked: " + note);
    }

    private static String content(GeneratedProject p, String path) {
        for (GeneratedProject.FileEntry f : p.files) if (f != null && path.equals(f.path)) return f.content == null ? "" : f.content;
        throw new IllegalStateException("missing generated file " + path);
    }

    private static String all(GeneratedProject p) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : p.files) if (f != null && f.content != null) b.append('\n').append(f.content);
        return b.toString();
    }
}
