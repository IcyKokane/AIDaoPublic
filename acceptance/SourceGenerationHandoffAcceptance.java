package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Regression for the real-device failure where an ordinary offline-capable
 * project reached PLAN READY but the workspace could only advance to
 * SOURCE BLOCKED. The workspace's source handoff is intentionally simple:
 * it may advance only when every generated verification note is non-failing.
 * These prompts therefore have to generate a complete locally-verifiable
 * source tree without requiring any remote provider or GitHub step.
 */
public final class SourceGenerationHandoffAcceptance {
    public static void main(String[] args) {
        verifyWorkoutCanAdvanceToSourceReady();
        verifyNotepadCanAdvanceToSourceReady();
        System.out.println("PASS source-generation handoff regressions");
    }

    private static void verifyWorkoutCanAdvanceToSourceReady() {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Simple Workout Tracker",
                "Create a simple workout tracking app. Should be able to track the exercise, weight and reps. It should have RPG type of UI, and it should show growth in the form of RPG stats. Workouts should be automatically in the app, not something that needs to be input.",
                Arrays.asList(
                        "Track exercise name, weight, and reps locally.",
                        "Calculate progression automatically from completed workouts.",
                        "Display RPG-style stats and growth without manual stat input."
                ),
                Arrays.asList("Generate workout model", "Generate tracker UI", "Calculate progression", "Validate source")
        );
        requireSourceReady(project, "workout");
        String all = join(project).toLowerCase();
        require(all, "workout_xp", "workout XP state before SOURCE READY");
        require(all, "stat_strength", "workout strength state before SOURCE READY");
        require(all, "stat_endurance", "workout endurance state before SOURCE READY");
        require(all, "store.text(\"workouts\"", "persisted workout mutation before SOURCE READY");
    }

    private static void verifyNotepadCanAdvanceToSourceReady() {
        GeneratedProject project = new LocalSourceGenerator().generate(
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
        requireSourceReady(project, "notepad");
        String all = join(project).toLowerCase();
        require(all, "android:icon=\"@drawable/ic_generated_app\"", "notepad launcher icon before SOURCE READY");
        require(all, "root.setorientation(linearlayout.horizontal)", "notepad sidebar layout before SOURCE READY");
        require(all, "note_lock_", "notepad lock state before SOURCE READY");
        require(all, "store.text(\"note_title_\"", "persisted note title before SOURCE READY");
        require(all, "store.text(\"note_body_\"", "persisted note body before SOURCE READY");
    }

    private static void requireSourceReady(GeneratedProject project, String label) {
        if (project == null) throw new IllegalStateException(label + " generator returned null");
        if (project.files == null || project.files.size() < 16)
            throw new IllegalStateException(label + " generated an incomplete source tree");
        for (String note : project.verificationNotes) {
            if (note != null && note.startsWith("FAIL "))
                throw new IllegalStateException(label + " would become SOURCE BLOCKED: " + note);
        }
    }

    private static String join(GeneratedProject project) {
        StringBuilder out = new StringBuilder();
        for (GeneratedProject.FileEntry file : project.files)
            if (file != null && file.content != null) out.append('\n').append(file.content);
        return out.toString();
    }

    private static void require(String source, String token, String label) {
        if (source == null || !source.contains(token))
            throw new IllegalStateException("Missing " + label + ": " + token);
    }
}
