package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.List;

/** Regression harness for real-device instruction-fidelity failures found during V1 testing. */
public final class GeneralRequestFidelityAcceptance {
    public static void main(String[] args) {
        verifyNotepadPrompt();
        verifyWorkoutPrompt();
        System.out.println("PASS general request fidelity regression harness");
    }

    private static void verifyNotepadPrompt() {
        List<String> requirements = Arrays.asList(
                "Use a sidebar to navigate between writing, editor, search, and library screens.",
                "Allow notes to be locked so locked notes cannot be edited until explicitly unlocked.",
                "Generate and wire a distinct launcher app logo.",
                "Use a modern purple and red visual theme.",
                "Persist saved notes and lock state locally."
        );
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Create A Notepad App That Uses A Sidebar To Navigate Different Screens",
                "Create a notepad app that uses a sidebar to navigate different screens. It should allow me to lock notes so they can't be edited. It should also have an app logo. It should use a modern UI that is purple and red.",
                requirements,
                Arrays.asList("Generate identity", "Generate UI", "Generate note persistence", "Validate fidelity")
        );
        assertNoFailure(project, "notepad");
        if (project.projectName.length() > 40 || project.projectName.toLowerCase().startsWith("create "))
            throw new IllegalStateException("Notepad request leaked into app identity: " + project.projectName);
        String all = join(project);
        require(all, "sidebar", "notepad sidebar navigation marker");
        requireAny(all, new String[]{"locked", "isLocked", "note_lock"}, "notepad lock-state marker");
        requireAny(all, new String[]{"setEnabled(false)", "setFocusable(false)", "setInputType(0)", "read-only", "read only"}, "locked-note edit prevention");
        requireAny(all, new String[]{"android:icon=", "android:roundIcon="}, "launcher icon declaration");
        requireAny(all.toLowerCase(), new String[]{"purple", "violet", "#6", "#7", "#8"}, "purple theme marker");
        requireAny(all.toLowerCase(), new String[]{"red", "crimson", "#e", "#f"}, "red theme marker");
    }

    private static void verifyWorkoutPrompt() {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Simple Workout Tracker",
                "Create a simple workout tracking app. It should track exercise, weight and reps. It should have an RPG type of UI and automatically show growth in the form of RPG stats; growth should be calculated by the app rather than manually entered.",
                Arrays.asList(
                        "Track exercise name, weight, and reps locally.",
                        "Calculate progression automatically from completed workouts.",
                        "Display RPG-style stats and growth without manual stat input."
                ),
                Arrays.asList("Generate workout model", "Generate tracker UI", "Calculate progression", "Validate source")
        );
        assertNoFailure(project, "workout");
        String all = join(project).toLowerCase();
        require(all, "exercise", "workout exercise field");
        require(all, "weight", "workout weight field");
        require(all, "reps", "workout reps field");
        requireAny(all, new String[]{"xp", "level", "stat", "growth"}, "workout RPG progression marker");
    }

    private static void assertNoFailure(GeneratedProject project, String label) {
        for (String note : project.verificationNotes)
            if (note.startsWith("FAIL ")) throw new IllegalStateException(label + " fidelity failure: " + note);
    }

    private static String join(GeneratedProject project) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : project.files) if (f != null && f.content != null) b.append('\n').append(f.content);
        return b.toString();
    }
    private static void require(String source, String token, String label) {
        if (!source.toLowerCase().contains(token.toLowerCase())) throw new IllegalStateException("Missing " + label + ": " + token);
    }
    private static void requireAny(String source, String[] tokens, String label) {
        for (String token : tokens) if (source.contains(token)) return;
        throw new IllegalStateException("Missing " + label);
    }
}
