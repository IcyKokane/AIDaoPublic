package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Guards against a subtle generated-app failure where mutating UI paths call the
 * LocalStore text getter with a value-shaped default instead of the persisted
 * string setter. It also protects the inverse failure: broad mutation-key
 * normalization must never turn read expressions into void putText(...) calls.
 */
public final class GeneratedMutationPersistenceAcceptance {
    public static void main(String[] args) {
        verifyNotepadMutationsPersist();
        verifyWorkoutMutationsPersist();
        System.out.println("PASS generated mutation persistence contract");
    }

    private static void verifyNotepadMutationsPersist() {
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
        assertNoFailure(project, "notepad");
        String store = content(project, "/LocalStore.java");
        require(store, "putText(String k,String v)", "LocalStore persisted string setter");
        String home = content(project, "/MainActivity.java");
        String editor = content(project, "/EditorActivity.java");
        String library = content(project, "/LibraryActivity.java");
        require(editor, "store.putText(\"note_title_\"+id", "note title persistence mutation");
        require(editor, "store.putText(\"note_body_\"+id", "note body persistence mutation");
        require(editor, "store.putText(\"documents\"", "note index persistence mutation");
        require(library, "store.putText(\"active_note\"", "active-note persistence mutation");

        // Read paths must remain getters after setter normalization. These exact
        // aliases previously regressed into putText(...) and produced invalid or
        // semantically broken generated source.
        require(home, "String docs=store.text(\"documents\",\"\")", "home document-index read");
        require(editor, "String id=store.text(\"active_note\",\"default\")", "active-note read");
        require(editor, "title.setText(store.text(\"note_title_\"+id,\"\"))", "note-title read");
        require(editor, "content.setText(store.text(\"note_body_\"+id,\"\"))", "note-body read");
        require(editor, "String docs=store.text(\"documents\",\"\")", "editor document-index read");
        require(library, "String raw=store.text(\"documents\",\"\")", "library document-index read");
        reject(home, "String docs=store.putText(\"documents\",\"\")", "home getter rewritten as setter");
        reject(editor, "String id=store.putText(\"active_note\",\"default\")", "active-note getter rewritten as setter");
        reject(library, "String raw=store.putText(\"documents\",\"\")", "library getter rewritten as setter");
    }

    private static void verifyWorkoutMutationsPersist() {
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
        String log = content(project, "/TimelineActivity.java");
        require(log, "store.putText(\"workouts\"", "workout history persistence mutation");
        require(log, "store.number(\"workout_xp\"", "workout XP mutation");
        require(log, "store.number(\"stat_strength\"", "strength-stat mutation");
        require(log, "store.number(\"stat_endurance\"", "endurance-stat mutation");
        require(log, "String old=store.text(\"workouts\",\"\")", "workout history read before append");
        reject(log, "String old=store.putText(\"workouts\",\"\")", "workout getter rewritten as setter");
    }

    private static void assertNoFailure(GeneratedProject project, String label) {
        for (String note : project.verificationNotes)
            if (note.startsWith("FAIL ")) throw new IllegalStateException(label + " fidelity failure: " + note);
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

    private static void reject(String source, String token, String label) {
        if (source != null && source.contains(token))
            throw new IllegalStateException("Found " + label + ": " + token);
    }
}
