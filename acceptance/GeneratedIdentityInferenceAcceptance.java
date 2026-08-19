package dev.thefoolish.aidao;

import java.util.Arrays;

/** Prevents sentence-like prompt text from leaking into launcher/product identity. */
public final class GeneratedIdentityInferenceAcceptance {
    public static void main(String[] args) {
        GeneratedProject genericWorkout = new LocalSourceGenerator().generate(
                "A simple workout tracking app",
                "Create a simple workout tracking app. Track exercise, weight and reps, with automatic RPG growth.",
                Arrays.asList("Track exercise, weight and reps", "Automatic RPG progression"),
                Arrays.asList("Generate", "Validate"));
        requireName(genericWorkout, "QuestFit");

        GeneratedProject explicit = new LocalSourceGenerator().generate(
                "Create a notepad app",
                "Create a notepad app called Ember Notes. It should have an app logo, sidebar navigation, purple and red styling, and lockable read-only notes.",
                Arrays.asList("App logo", "Sidebar", "Purple and red", "Lock notes"),
                Arrays.asList("Generate", "Validate"));
        requireName(explicit, "Ember Notes");

        GeneratedProject concise = new LocalSourceGenerator().generate(
                "LiftQuest",
                "Create a workout app that tracks exercise, weight and reps with automatic RPG stat growth.",
                Arrays.asList("Workout tracking", "Automatic RPG stats"),
                Arrays.asList("Generate", "Validate"));
        requireName(concise, "LiftQuest");

        System.out.println("PASS generated app identity inference acceptance");
    }

    private static void requireName(GeneratedProject project, String expected) {
        if (project == null || !expected.equals(project.projectName))
            throw new IllegalStateException("Expected app identity '" + expected + "' but got '" + (project == null ? "null" : project.projectName) + "'");
        for (String note : project.verificationNotes)
            if (note.startsWith("FAIL ")) throw new IllegalStateException("Identity case failed fidelity gate: " + note);
        String manifest = content(project, "app/src/main/AndroidManifest.xml");
        String strings = content(project, "app/src/main/res/values/strings.xml");
        if (!manifest.contains("android:label=\"" + expected + "\""))
            throw new IllegalStateException("Manifest label not synchronized with inferred identity: " + expected);
        if (!strings.contains(">" + expected + "</string>"))
            throw new IllegalStateException("app_name resource not synchronized with inferred identity: " + expected);
    }

    private static String content(GeneratedProject project, String path) {
        for (GeneratedProject.FileEntry f : project.files)
            if (f != null && path.equals(f.path)) return f.content == null ? "" : f.content;
        throw new IllegalStateException("Missing generated file " + path);
    }
}
