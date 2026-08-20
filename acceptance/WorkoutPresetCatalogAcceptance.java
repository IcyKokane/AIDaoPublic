package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Regression for the real-device workout prompt. The user explicitly asked for
 * workouts/exercises to already exist in the app rather than requiring manual
 * exercise-name entry. Weight and reps remain user-entered measurements, while
 * exercise selection must come from a generated built-in catalog. Tracking also
 * requires persisted workout rows to be rendered back to the user after restart;
 * silently storing the records is not sufficient product behavior.
 */
public final class WorkoutPresetCatalogAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Simple Workout Tracker",
                "Create a simple workout tracking app. Should be able to track the exercise, weight and reps. It should have RPG type of UI, and it should show growth in the form of RPG stats. Workouts should be automatically in the app, not something that needs to be input.",
                Arrays.asList(
                        "Track exercise name, weight, and reps locally.",
                        "Provide workouts/exercises in the app so the exercise name does not need to be manually typed.",
                        "Calculate progression automatically from completed workouts.",
                        "Display RPG-style stats and growth without manual stat input."
                ),
                Arrays.asList("Generate workout catalog", "Generate tracker UI", "Calculate progression", "Validate source")
        );

        for (String note : project.verificationNotes) {
            if (note != null && note.startsWith("FAIL "))
                throw new IllegalStateException("workout fidelity failure: " + note);
        }

        String log = content(project, "/TimelineActivity.java");
        require(log, "Spinner", "preset exercise selector");
        require(log, "ArrayAdapter", "preset exercise adapter");
        requireAny(log, new String[]{"Squat", "Bench Press", "Deadlift", "Push Up", "Pull Up"},
                "at least one concrete built-in exercise");
        forbid(log, "EditText exercise=field(\"Exercise\")",
                "manual exercise-name entry when the prompt requests built-in workouts");
        require(log, "Weight", "weight input");
        require(log, "Reps", "reps input");
        require(log, "store.putText(\"workouts\"", "persisted workout history mutation");
        require(log, "store.text(\"workouts\"", "persisted workout history readback");
        requireAny(log, new String[]{"Recent workouts", "Workout history", "Recent sets", "History"},
                "visible workout history section");
        requireAny(log, new String[]{"split(\"\\n\")", "split(\"\\\\n\")"},
                "saved workout row iteration");
        require(log, "workout_xp", "automatic RPG progression");

        System.out.println("PASS preset workout catalog and visible persisted history fidelity");
    }

    private static String content(GeneratedProject project, String suffix) {
        for (GeneratedProject.FileEntry f : project.files)
            if (f != null && f.path != null && f.path.endsWith(suffix)) return f.content == null ? "" : f.content;
        throw new IllegalStateException("Missing generated file " + suffix);
    }

    private static void require(String source, String token, String label) {
        if (source == null || !source.contains(token))
            throw new IllegalStateException("Missing " + label + ": " + token);
    }

    private static void requireAny(String source, String[] tokens, String label) {
        for (String token : tokens) if (source != null && source.contains(token)) return;
        throw new IllegalStateException("Missing " + label);
    }

    private static void forbid(String source, String token, String label) {
        if (source != null && source.contains(token))
            throw new IllegalStateException("Found forbidden " + label + ": " + token);
    }
}
