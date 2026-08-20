package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Guards the generated LocalStore string API against getter/setter ambiguity.
 * Real-device fidelity overrides must persist through putText while reads remain text.
 */
public final class StableGeneratedLocalStoreContractAcceptance {
    public static void main(String[] args) {
        verify("notepad", new LocalSourceGenerator().generate(
                "Create A Notepad App That Uses A Sidebar",
                "Create a notepad app that uses a sidebar to navigate different screens. It should allow me to lock notes so they can't be edited. It should also have a app logo. It should use a modern UI that is purple and red.",
                Arrays.asList("Sidebar navigation", "Lock notes", "App logo", "Purple and red modern UI"),
                Arrays.asList("Generate notes app")),
                new String[]{"documents", "active_note", "note_title_", "note_body_"});

        verify("workout", new LocalSourceGenerator().generate(
                "Create A Simple Workout Tracking App",
                "Create a simple workout tracking app. Track exercise, weight and reps. Use an RPG type UI and show automatic RPG stats. Workouts should already be in the app, not something that needs to be input.",
                Arrays.asList("Track exercise weight reps", "RPG stats", "Built-in workouts"),
                Arrays.asList("Generate workout app")),
                new String[]{"workout_history"});

        verify("pantry", new LocalSourceGenerator().generate(
                "Create A Pantry Inventory App Called PantryQuest",
                "Create a pantry inventory app called PantryQuest. Use top tab navigation. Use a modern teal and orange theme. Let me add pantry items with a quantity, change the quantity later, and keep the inventory after restarting the app.",
                Arrays.asList("App name is PantryQuest", "Top tabs", "Teal orange", "Persist quantity"),
                Arrays.asList("Generate pantry app")),
                new String[]{"pantry_inventory"});

        GeneratedProject generic = new LocalSourceGenerator().generate(
                "Create A Simple Grocery List App",
                "Create a simple grocery list app that works offline. Let me add and edit items and keep the list after restarting the app.",
                Arrays.asList("Offline grocery list", "Add and edit items", "Persist data after restart"),
                Arrays.asList("Generate grocery list app"));
        verify("generic-offline", generic, new String[]{"last_surface"});
        if (!"Grocery List".equals(generic.projectName))
            throw new IllegalStateException("generic offline identity was not normalized: " + generic.projectName);

        System.out.println("PASS stable generated LocalStore contract");
    }

    private static void verify(String label, GeneratedProject project, String[] mutationKeys) {
        for (String note : project.verificationNotes) {
            if (note.startsWith("FAIL ")) throw new IllegalStateException(label + " source blocked: " + note);
        }
        String store = suffix(project, "/LocalStore.java");
        if (!store.contains("public String text(String k,String d)"))
            throw new IllegalStateException(label + " missing text getter");
        if (!store.contains("public void putText(String k,String v)"))
            throw new IllegalStateException(label + " missing putText setter");
        if (store.contains("public void text(String k,String v)"))
            throw new IllegalStateException(label + " still exposes ambiguous text setter");

        String all = all(project);
        for (String key : mutationKeys) {
            if (!all.contains("store.putText(\"" + key))
                throw new IllegalStateException(label + " mutation does not use putText for " + key);
        }
        if (all.contains("=store.putText(") || all.contains("setText(store.putText("))
            throw new IllegalStateException(label + " read expression was rewritten into void putText setter");
    }

    private static String suffix(GeneratedProject project, String suffix) {
        for (GeneratedProject.FileEntry f : project.files) {
            if (f != null && f.path != null && f.path.endsWith(suffix)) return f.content == null ? "" : f.content;
        }
        throw new IllegalStateException("Missing generated file " + suffix);
    }

    private static String all(GeneratedProject project) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : project.files) if (f != null && f.content != null) b.append('\n').append(f.content);
        return b.toString();
    }
}
