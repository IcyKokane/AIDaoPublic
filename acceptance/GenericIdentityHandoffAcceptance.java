package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Regression for prompt-like project identity outside the notepad/workout special cases.
 * Ordinary local-capable requests must not become SOURCE BLOCKED simply because the
 * initial project title was copied from the user's sentence.
 */
public final class GenericIdentityHandoffAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Create A Simple Grocery List App That Lets Me Check Items Off",
                "Create a simple grocery list app that lets me add items, check them off, and keep the list after restarting the app.",
                Arrays.asList(
                        "Add grocery items locally.",
                        "Allow items to be checked and unchecked.",
                        "Persist the list across restart."
                ),
                Arrays.asList("Generate local model", "Generate list UI", "Persist state", "Validate source")
        );

        for (String note : project.verificationNotes) {
            if (note.startsWith("FAIL ")) {
                throw new IllegalStateException("generic offline request would be SOURCE BLOCKED: " + note);
            }
        }

        String name = project.projectName == null ? "" : project.projectName.trim();
        String lower = name.toLowerCase();
        int words = name.isEmpty() ? 0 : name.split("\\s+").length;
        if (name.length() < 2 || name.length() > 32 || words > 5 ||
                lower.startsWith("create ") || lower.startsWith("make ") || lower.startsWith("build ") ||
                lower.contains(" should ") || name.endsWith(".")) {
            throw new IllegalStateException("generic prompt leaked into product identity: " + name);
        }

        String strings = content(project, "app/src/main/res/values/strings.xml");
        String manifest = content(project, "app/src/main/AndroidManifest.xml");
        if (!strings.contains(">" + name + "</string>")) {
            throw new IllegalStateException("app_name is not synchronized with inferred generic product identity");
        }
        if (!manifest.contains("android:label=\"" + name + "\"")) {
            throw new IllegalStateException("manifest label is not synchronized with inferred generic product identity");
        }

        System.out.println("PASS generic identity/source-handoff regression");
    }

    private static String content(GeneratedProject project, String path) {
        for (GeneratedProject.FileEntry f : project.files) {
            if (f != null && path.equals(f.path)) return f.content == null ? "" : f.content;
        }
        throw new IllegalStateException("Missing generated file " + path);
    }
}
