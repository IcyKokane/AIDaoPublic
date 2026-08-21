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
        String identity = project.projectName == null ? "" : project.projectName.trim();
        if (identity.length() < 2 || identity.length() > 32 || identity.toLowerCase().startsWith("create ") || identity.toLowerCase().contains(" should "))
            throw new IllegalStateException("Notepad request leaked into app identity: " + identity);
        if ("Create A Notepad App That Uses A Sidebar To Navigate Different Screens".equalsIgnoreCase(identity))
            throw new IllegalStateException("Raw prompt title was reused as notepad product identity");

        String manifest = content(project, "app/src/main/AndroidManifest.xml");
        String colors = content(project, "app/src/main/res/values/colors.xml");
        String icon = content(project, "/ic_generated_app.xml");
        String appScreen = content(project, "/AppScreen.java");
        String home = content(project, "/MainActivity.java");
        String editor = content(project, "/EditorActivity.java");
        String library = content(project, "/LibraryActivity.java");

        require(manifest, "android:icon=\"@drawable/ic_generated_app\"", "launcher icon declaration");
        require(manifest, "android:roundIcon=\"@drawable/ic_generated_app\"", "round launcher icon declaration");
        require(icon, "<vector", "generated launcher vector");
        require(icon, "<path", "non-empty generated launcher artwork");
        require(colors.toUpperCase(), "#7C3AED", "explicit purple theme accent");
        require(colors.toUpperCase(), "#EF4444", "explicit red theme accent");
        require(appScreen, "root.setOrientation(LinearLayout.HORIZONTAL)", "sidebar root orientation");
        require(appScreen, "root.addView(nav,new LinearLayout.LayoutParams(dp(104),-1))", "sidebar occupies a visible side rail");
        require(appScreen, "q.setBackground(round(ACCENT,14))", "requested purple accent is materially applied to primary actions");
        require(appScreen, "brand.setTextColor(SECONDARY)", "requested secondary red accent is materially applied to branding");
        requireAny(appScreen, new String[]{"Writing", "Editor", "Search", "Library"}, "requested notepad navigation destinations");
        require(home, "System.currentTimeMillis()", "new-note identity generation");
        require(home, "store.putText(\"active_note\"", "new-note active identity persistence");
        requireAny(editor, new String[]{"note_lock_", "locked"}, "notepad lock-state marker");
        requireAny(editor, new String[]{"setEnabled(false)", "setFocusable(false)", "setInputType(0)"}, "locked-note edit prevention");
        require(editor, "store.putText(\"note_title_\"", "note title persistence mutation");
        require(editor, "store.putText(\"note_body_\"", "note body persistence mutation");
        require(editor, "store.putText(\"documents\"", "note library persistence mutation");
        require(library, "store.putText(\"active_note\"", "saved-note reopen persistence mutation");
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
        require(all, "workout_xp", "workout XP progression state");
        require(all, "stat_strength", "workout strength progression state");
        require(all, "stat_endurance", "workout endurance progression state");
        requireAny(all, new String[]{"complete set", "completed set", "save workout"}, "completed workout mutation action");
        String appScreen = content(project, "/AppScreen.java");
        String log = content(project, "/TimelineActivity.java");
        require(appScreen, "root.addView(main,new LinearLayout.LayoutParams(-1,0,1))", "non-sidebar main content receives visible width and weighted height");
        require(log, "store.putText(\"workout_history\"", "completed workout history persistence");
        requireAny(log, new String[]{"store.number(\"workout_xp\"", "workout_xp"}, "automatic XP mutation from workout completion");
        requireAny(log, new String[]{"stat_strength", "stat_endurance"}, "automatic RPG stat mutation from workout completion");
        verifyLocalStoreApiCompatibility(project, "workout");
    }

    private static void verifyLocalStoreApiCompatibility(GeneratedProject project, String label) {
        String store = content(project, "/LocalStore.java");
        String all = join(project);
        if (all.contains("store.putText(") && !store.contains("putText("))
            throw new IllegalStateException(label + " generated source calls LocalStore.putText but LocalStore does not declare putText");
        if (all.contains("store.putNumber(") && !store.contains("putNumber("))
            throw new IllegalStateException(label + " generated source calls LocalStore.putNumber but LocalStore does not declare putNumber");
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
            if (f != null && f.path != null && (f.path.equals(suffix) || f.path.endsWith(suffix))) return f.content == null ? "" : f.content;
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
