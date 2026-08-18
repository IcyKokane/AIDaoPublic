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
        require(all, "sideNav()", "notepad sidebar navigation marker");
        requireAny(all, new String[]{"locked", "isLocked", "note_lock"}, "notepad lock-state marker");
        require(all, "setEnabled(!locked)", "locked-note control disabling");
        require(all, "setFocusable(!locked)", "locked-note edit prevention");
        requireAny(all, new String[]{"android:icon=", "android:roundIcon="}, "launcher icon declaration");
        require(all.toLowerCase(), "#7c3aed", "purple theme token");
        require(all.toLowerCase(), "#ef4444", "red theme token");
        require(all, "note_title_", "note title persistence");
        require(all, "note_body_", "note body persistence");
        require(all, "documents", "note library persistence");
        String appScreen = content(project, "/AppScreen.java");
        require(appScreen, "root.addView(nav,new LinearLayout.LayoutParams(dp(112),-1))", "sidebar occupies a visible side rail");
        require(content(project, "ic_generated_app.xml"), "pathData", "generated launcher art");
        verifyLocalStoreApiCompatibility(project, "notepad");
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
        require(all, "workout_xp", "automatic workout XP persistence");
        require(all, "stat_strength", "automatic strength progression");
        require(all, "stat_endurance", "automatic endurance progression");
        String appScreen = content(project, "/AppScreen.java");
        require(appScreen, "root.addView(main,new LinearLayout.LayoutParams(-1,0,1))", "non-sidebar main content receives visible width and weighted height");
        verifyLocalStoreApiCompatibility(project, "workout");
    }

    private static void verifyLocalStoreApiCompatibility(GeneratedProject project, String label) {
        String store = content(project, "/LocalStore.java");
        String all = join(project);
        if (all.contains("store.putText(") && !store.contains("putText("))
            throw new IllegalStateException(label + " generated source calls LocalStore.putText but LocalStore does not declare putText");
        if (all.contains("store.putNumber(") && !store.contains("putNumber("))
            throw new IllegalStateException(label + " generated source calls LocalStore.putNumber but LocalStore does not declare putNumber");
        if (all.contains("store.number(") && !store.contains("number("))
            throw new IllegalStateException(label + " generated source calls LocalStore.number but LocalStore does not declare number");
        if (all.contains("store.flag(") && !store.contains("flag("))
            throw new IllegalStateException(label + " generated source calls LocalStore.flag but LocalStore does not declare flag");
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
    private static String content(GeneratedProject project, String suffix) {
        for (GeneratedProject.FileEntry f : project.files)
            if (f != null && f.path != null && f.path.endsWith(suffix)) return f.content == null ? "" : f.content;
        throw new IllegalStateException("Missing generated file " + suffix);
    }
    private static void require(String source, String token, String label) {
        if (!source.toLowerCase().contains(token.toLowerCase())) throw new IllegalStateException("Missing " + label + ": " + token);
    }
    private static void requireAny(String source, String[] tokens, String label) {
        for (String token : tokens) if (source.contains(token)) return;
        throw new IllegalStateException("Missing " + label);
    }
}
