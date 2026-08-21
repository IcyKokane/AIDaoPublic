package dev.thefoolish.aidao;

import java.util.Arrays;

/** Verifies the real-device notepad request emits a persisted editor lock/read-only contract. */
public final class ExplicitNoteLockContractAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Create A Notepad App That Uses A Sidebar To Navigate Different Screens",
                "Create a notepad app that uses a sidebar to navigate different screens. It should allow me to lock notes so they can't be edited. It should also have an app logo. It should use a modern UI that is purple and red.",
                Arrays.asList("Use a sidebar", "Lock notes", "Generate app logo", "Purple and red modern UI"),
                Arrays.asList("Generate", "Validate"));
        String editor = "";
        for (GeneratedProject.FileEntry file : project.files) {
            if (file != null && file.path != null && file.path.endsWith("/EditorActivity.java")) editor = file.content == null ? "" : file.content;
        }
        require(editor.contains("store.flag(\"note_lock_\"+id)"), "editor does not read persisted note lock state");
        require(editor.contains("store.flag(\"note_lock_\"+id,!store.flag(\"note_lock_\"+id))"), "editor does not persist lock/unlock mutation");
        require(editor.contains("setEnabled(false)") && editor.contains("setFocusable(false)"), "locked note does not become read-only");
        require(editor.contains("Unlock note") && editor.contains("Lock note"), "lock state is not explicitly reversible");
        System.out.println("PASS explicit note-lock editor contract");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
